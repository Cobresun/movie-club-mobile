package cobresun.movieclub.app.watchlist.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _state = MutableStateFlow<WatchListState>(WatchListState.Loading)
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
                _state.update { WatchListState.Loaded(watchList = watchList) }
            }
            .onError { error ->
                _state.update {
                    WatchListState.Error(errorMessage = error.toString())
                }
            }
    }
}

sealed class WatchListState {
    data object Loading : WatchListState()
    data class Loaded(val watchList: List<WatchListItem>) : WatchListState()
    data class Error(val errorMessage: String) : WatchListState()
}
