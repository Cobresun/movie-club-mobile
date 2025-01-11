package cobresun.movieclub.app.watchlist.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.domain.WatchListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WatchListViewModel(
    private val watchListRepository: WatchListRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _state = MutableStateFlow(WatchListState())
    val state = _state.asStateFlow()
        .onStart {
            getWatchList()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    private fun getWatchList() = viewModelScope.launch {
        watchListRepository.getWatchList(clubId)
            .onSuccess { watchList ->
                _state.update { it.copy(watchList = AsyncResult.Success(watchList)) }
            }
            .onError { error ->
                _state.update { it.copy(watchList = AsyncResult.Error()) }
            }
    }
}

data class WatchListState(
    val watchList: AsyncResult<List<WatchListItem>> = AsyncResult.Loading
)
