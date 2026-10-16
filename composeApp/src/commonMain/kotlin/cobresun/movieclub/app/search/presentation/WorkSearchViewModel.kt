package cobresun.movieclub.app.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.search.domain.WorkSearchRepository
import cobresun.movieclub.app.search.domain.WorkSearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkSearchState(
    val workType: WorkType? = null,
    val query: String = "",
    val suggestions: AsyncResult<List<WorkSearchResult>> = AsyncResult.Loading,
    /** Null while the query is blank (suggestions are shown instead). */
    val results: AsyncResult<List<WorkSearchResult>>? = null,
    val errorMessage: String? = null,
)

/** Debounced search against TMDB / Google Books for the add-work sheet. */
@OptIn(FlowPreview::class)
class WorkSearchViewModel(
    private val repository: WorkSearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(WorkSearchState())
    val state = _state.asStateFlow()

    private val queries = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            queries
                .map { it.trim() }
                .drop(1)
                .debounce(350)
                .distinctUntilChanged()
                .collect { runSearch(it) }
        }
    }

    /** Prepares the sheet for a club's media type; loads suggestions once per type. */
    fun start(type: WorkType) {
        val previous = _state.value.workType
        _state.update { it.copy(workType = type, query = "", results = null, errorMessage = null) }
        queries.value = ""
        if (previous != type || _state.value.suggestions !is AsyncResult.Success) loadSuggestions(type)
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, results = if (query.isBlank()) null else it.results) }
        queries.value = query
    }

    fun retry() {
        val type = _state.value.workType ?: return
        if (_state.value.query.isBlank()) loadSuggestions(type) else runSearch(_state.value.query.trim())
    }

    private fun loadSuggestions(type: WorkType) {
        viewModelScope.launch {
            _state.update { it.copy(suggestions = AsyncResult.Loading) }
            when (val result = repository.suggestions(type)) {
                is Result.Success -> _state.update { it.copy(suggestions = AsyncResult.Success(result.data)) }
                is Result.Error -> _state.update {
                    it.copy(
                        suggestions = AsyncResult.Error(dataError = result.error),
                        errorMessage = result.error.message(type, "load suggestions"),
                    )
                }
            }
        }
    }

    private fun runSearch(query: String) {
        val type = _state.value.workType ?: return
        searchJob?.cancel()
        if (query.isEmpty()) {
            _state.update { it.copy(results = null) }
            return
        }
        searchJob = viewModelScope.launch {
            _state.update { it.copy(results = AsyncResult.Loading) }
            when (val result = repository.search(type, query)) {
                is Result.Success -> _state.update { it.copy(results = AsyncResult.Success(result.data), errorMessage = null) }
                is Result.Error -> _state.update {
                    it.copy(
                        results = AsyncResult.Error(dataError = result.error),
                        errorMessage = result.error.message(type, "search"),
                    )
                }
            }
        }
    }
}

/** Keyless Google Books has a small shared quota; say so rather than a generic rate-limit message. */
private fun DataError.Remote.message(type: WorkType, action: String): String =
    if (this == DataError.Remote.TOO_MANY_REQUESTS && type == WorkType.BOOK) {
        "Google Books is busy right now. Wait a minute and try again."
    } else {
        toUserMessage(action)
    }
