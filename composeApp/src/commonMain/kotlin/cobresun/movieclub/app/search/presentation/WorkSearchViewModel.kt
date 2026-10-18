package cobresun.movieclub.app.search.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import cobresun.movieclub.app.core.domain.toAsyncResult
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.search.domain.BrowseTab
import cobresun.movieclub.app.search.domain.WorkSearchRepository
import cobresun.movieclub.app.search.domain.WorkSearchResult
import cobresun.movieclub.app.search.domain.browseTabs
import cobresun.movieclub.app.search.domain.supportsRecommendations
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

/** A browse tab's pages so far (web `useBrowse`'s infinite query). */
data class BrowseState(
    val results: AsyncResult<List<WorkSearchResult>> = AsyncResult.Loading,
    val nextPage: Int? = null,
    val isLoadingMore: Boolean = false,
)

data class WorkSearchState(
    val workType: WorkType? = null,
    val query: String = "",
    /** Browse tabs for this media type; empty when the sheet doesn't browse (Add review). */
    val tabs: List<BrowseTab> = emptyList(),
    val selectedTab: String? = null,
    /** Movie clubs adding to a list get a Recommended tab first (web `supportsRecommendations`). */
    val offersRecommendations: Boolean = false,
    val recommendedSelected: Boolean = false,
    /** Null until the Recommended tab is first shown. */
    val recommendations: AsyncResult<List<WorkSearchResult>>? = null,
    val browse: Map<String, BrowseState> = emptyMap(),
    /** Null while the query is blank. */
    val results: AsyncResult<List<WorkSearchResult>>? = null,
    val errorMessage: String? = null,
) {
    val showingRecommendations: Boolean get() = offersRecommendations && recommendedSelected

    /** The browse tab on screen, or null while Recommended is. */
    val selectedBrowse: BrowseState? get() = if (showingRecommendations) null else selectedTab?.let { browse[it] }

    val selectedTabLabel: String? get() = tabs.firstOrNull { it.key == selectedTab }?.label
}

/**
 * The add sheet's catalogue (web `AddWorkModal` + `WorkSearchPrompt`): browse
 * tabs paged from TMDB / Google Books, plus debounced search as you type.
 */
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

    /**
     * Prepares the sheet for a club's media type. [browsing] shows the browse
     * tabs (adding to a list); Add review only searches, like web.
     */
    fun start(type: WorkType, browsing: Boolean, clubSlug: String? = null) {
        val tabs = if (browsing) browseTabs(type) else emptyList()
        val keepBrowse = _state.value.workType == type
        val recommends = browsing && clubSlug != null && type.supportsRecommendations
        // Refetch each time the sheet opens (web invalidates them after every add).
        _state.update { it.copy(recommendations = null) }
        this.clubSlug = clubSlug
        _state.update {
            it.copy(
                workType = type,
                query = "",
                results = null,
                errorMessage = null,
                tabs = tabs,
                selectedTab = it.selectedTab?.takeIf { key -> keepBrowse && tabs.any { t -> t.key == key } } ?: tabs.firstOrNull()?.key,
                browse = if (keepBrowse) it.browse else emptyMap(),
                offersRecommendations = recommends,
                // Like web, the sheet opens on Recommended when it's offered.
                recommendedSelected = recommends,
            )
        }
        queries.value = ""
        if (recommends) ensureRecommendations() else _state.value.selectedTab?.let { ensureLoaded(it) }
    }

    fun selectTab(key: String) {
        _state.update { it.copy(selectedTab = key, recommendedSelected = false) }
        ensureLoaded(key)
    }

    fun selectRecommended() {
        _state.update { it.copy(recommendedSelected = true) }
        ensureRecommendations()
    }

    private var clubSlug: String? = null

    private fun ensureRecommendations() {
        val slug = clubSlug ?: return
        if (_state.value.recommendations is AsyncResult.Success || _state.value.recommendations is AsyncResult.Loading) return
        _state.update { it.copy(recommendations = AsyncResult.Loading) }
        viewModelScope.launch {
            val result = repository.recommendations(slug)
            _state.update {
                it.copy(
                    recommendations = result.map { it.map { r -> r.work } }.toAsyncResult()
                )
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query, results = if (query.isBlank()) null else it.results) }
        queries.value = query
    }

    /** The next page of the selected tab (infinite scroll; web "Load More"). */
    fun loadMore() {
        val key = _state.value.selectedTab ?: return
        val tab = _state.value.browse[key] ?: return
        val next = tab.nextPage ?: return
        if (tab.isLoadingMore || tab.results !is AsyncResult.Success) return
        loadPage(key, next)
    }

    fun retry() {
        if (_state.value.showingRecommendations && _state.value.recommendations is AsyncResult.Error) {
            _state.update { it.copy(recommendations = null) }
            ensureRecommendations()
        }
        val query = _state.value.query.trim()
        if (query.isNotEmpty()) runSearch(query)
        _state.value.selectedTab?.let { key ->
            if (_state.value.browse[key]?.results is AsyncResult.Error) {
                _state.update { it.copy(browse = it.browse - key) }
                ensureLoaded(key)
            }
        }
    }

    private fun ensureLoaded(key: String) {
        if (_state.value.browse[key]?.results is AsyncResult.Success) return
        loadPage(key, 1)
    }

    private fun loadPage(key: String, page: Int) {
        val type = _state.value.workType ?: return
        updateBrowse(key) { if (page == 1) BrowseState() else it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            when (val result = repository.browse(type, key, page)) {
                is Result.Success -> updateBrowse(key) { current ->
                    val before = if (page == 1) emptyList() else (current.results as? AsyncResult.Success)?.data.orEmpty()
                    val seen = before.mapTo(HashSet()) { it.externalId }
                    BrowseState(
                        // TMDB pages can repeat a title across pages; keep grid keys unique.
                        results = AsyncResult.Success(before + result.data.results.filter { seen.add(it.externalId) }),
                        nextPage = result.data.nextPage,
                    )
                }
                is Result.Error -> {
                    if (page == 1) {
                        updateBrowse(key) { BrowseState(results = AsyncResult.Error(dataError = result.error)) }
                    } else {
                        updateBrowse(key) { it.copy(isLoadingMore = false) }
                    }
                    _state.update { it.copy(errorMessage = result.error.message(type, "load more")) }
                }
            }
        }
    }

    private fun updateBrowse(key: String, transform: (BrowseState) -> BrowseState) {
        _state.update { it.copy(browse = it.browse + (key to transform(it.browse[key] ?: BrowseState()))) }
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
