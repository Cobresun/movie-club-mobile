package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.reviews.domain.NewReviewItem
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.tmdb.domain.TmdbRepository
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.domain.WatchListRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClubViewModel(
    savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val watchListRepository: WatchListRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _state = MutableStateFlow(ClubState())
    val state = _state.asStateFlow()

    init {
        loadReviews()
        loadWatchList()
        loadBacklog()
        loadTrendingMovies()
    }

    private fun loadReviews() {
        viewModelScope.launch {
            _state.update { it.copy(reviews = AsyncResult.Loading) }
            when (val result = reviewsRepository.getReviews(clubId)) {
                is Result.Success -> {
                    _state.update { it.copy(reviews = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(reviews = AsyncResult.Error()) }
                }
            }
        }
    }

    private fun loadWatchList() {
        viewModelScope.launch {
            _state.update { it.copy(watchList = AsyncResult.Loading) }
            when (val result = watchListRepository.getWatchList(clubId)) {
                is Result.Success -> {
                    _state.update { it.copy(watchList = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(watchList = AsyncResult.Error()) }
                }
            }
        }
    }

    private fun loadBacklog() {
        viewModelScope.launch {
            _state.update { it.copy(backlog = AsyncResult.Loading) }
            when (val result = watchListRepository.getBacklog(clubId)) {
                is Result.Success -> {
                    _state.update { it.copy(backlog = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(backlog = AsyncResult.Error()) }
                }
            }
        }
    }

    private fun loadTrendingMovies() {
        viewModelScope.launch {
            _state.update { it.copy(trendingMovies = AsyncResult.Loading) }
            when (val result = tmdbRepository.getTrendingMovies()) {
                is Result.Success -> {
                    _state.update { it.copy(trendingMovies = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(trendingMovies = AsyncResult.Error()) }
                }
            }
        }
    }

    fun onAction(action: ClubAction) {
        when (action) {
            is ClubAction.OnAddMovieToWatchList -> {
                viewModelScope.launch {
                    watchListRepository.postWatchListFromMovie(clubId, action.movie)
                        .onSuccess {
                            loadBacklog()
                        }
                        .onError {
                            _errorMessage.update { "Failed to add movie" }
                        }
                }
            }

            is ClubAction.OnAddMovieToBacklog -> {
                viewModelScope.launch {
                    watchListRepository.postBacklog(clubId, action.movie)
                        .onSuccess {
                            loadBacklog()
                        }
                        .onError {
                            _errorMessage.update { "Failed to add movie to backlog" }
                        }
                }
            }

            is ClubAction.OnDeleteBacklogItem -> {
                viewModelScope.launch {
                    watchListRepository.deleteBacklog(clubId, action.item.id)
                        .onSuccess {
                            loadBacklog()
                        }
                        .onError {
                            _errorMessage.update { "Failed to delete item" }
                        }
                }
            }

            is ClubAction.OnDeleteWatchListItem -> {
                viewModelScope.launch {
                    watchListRepository.deleteWatchList(clubId, action.item.id)
                        .onSuccess {
                            loadWatchList()
                        }
                        .onError {
                            _errorMessage.update { "Failed to delete item" }
                        }
                }
            }

            is ClubAction.OnMoveToWatchList -> {
                viewModelScope.launch {
                    watchListRepository.postWatchList(clubId, action.watchListItem)
                        .onSuccess {
                            watchListRepository.deleteBacklog(clubId, action.watchListItem.id)
                                .onSuccess {
                                    loadWatchList()
                                    loadBacklog()
                                }
                                .onError {
                                    _errorMessage.update { "Failed to move item" }
                                }
                        }
                        .onError {
                            _errorMessage.update { "Failed to move item" }
                        }
                }
            }

            is ClubAction.OnMoveToReview -> {
                viewModelScope.launch {
                    reviewsRepository.postReview(
                        clubId,
                        NewReviewItem(
                            type = action.watchListItem.type.value,
                            title = action.watchListItem.title,
                            externalId = action.watchListItem.externalId,
                            imageUrl = action.watchListItem.imageUrl
                        )
                    )
                        .onSuccess {
                            watchListRepository.deleteWatchList(clubId, action.watchListItem.id)
                                .onSuccess {
                                    loadReviews()
                                    loadWatchList()
                                }
                                .onError {
                                    _errorMessage.update { "Failed to create review" }
                                }
                        }
                        .onError {
                            _errorMessage.update { "Failed to create review" }
                        }
                }
            }

            is ClubAction.OnDeleteReview -> {
                viewModelScope.launch {
                    reviewsRepository.deleteReview(clubId, action.reviewId)
                        .onSuccess {
                            loadReviews()
                        }
                        .onError {
                            _errorMessage.update { "Failed to delete review" }
                        }
                }
            }

            is ClubAction.OnClearError -> {
                _errorMessage.update { null }
            }
        }
    }

}

sealed interface ClubAction {
    data class OnAddMovieToWatchList(val movie: TmdbMovie) : ClubAction
    data class OnAddMovieToBacklog(val movie: TmdbMovie) : ClubAction
    data class OnDeleteWatchListItem(val item: WatchListItem) : ClubAction
    data class OnDeleteBacklogItem(val item: WatchListItem) : ClubAction
    data class OnMoveToWatchList(val watchListItem: WatchListItem) : ClubAction
    data class OnMoveToReview(val watchListItem: WatchListItem) : ClubAction
    data class OnDeleteReview(val reviewId: String) : ClubAction
    data object OnClearError : ClubAction
}

data class ClubState(
    val reviews: AsyncResult<List<Review>> = AsyncResult.Loading,
    val watchList: AsyncResult<List<WatchListItem>> = AsyncResult.Loading,
    val backlog: AsyncResult<List<WatchListItem>> = AsyncResult.Loading,
    val trendingMovies: AsyncResult<List<TmdbMovie>> = AsyncResult.Loading
)
