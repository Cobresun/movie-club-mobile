package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ClubViewModel(
    savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val watchListRepository: WatchListRepository,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _state = MutableStateFlow(ClubState())
    val state = _state.asStateFlow()
        .onStart {
            getReviews()
            getWatchList()
            getBacklog()
            getTrendingMovies()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    fun onAction(action: ClubAction) {
        when (action) {
            is ClubAction.OnAddMovieToWatchList -> {
                TODO("Not yet implemented")
            }

            is ClubAction.OnAddMovieToBacklog -> {
                viewModelScope.launch {
                    watchListRepository.postBacklog(clubId, action.movie)
                        .onSuccess {
                            getBacklog()
                        }
                        .onError {
                            println("Error adding movie to backlog: $it")
                        }
                }
            }

            is ClubAction.OnDeleteBacklogItem -> {
                viewModelScope.launch {
                    watchListRepository.deleteBacklog(clubId, action.item.id)
                        .onSuccess {
                            getBacklog()
                        }
                        .onError {
                            println("Error deleting movie from backlog: $it")
                        }
                }
            }

            is ClubAction.OnDeleteWatchListItem -> {
                viewModelScope.launch {
                    watchListRepository.deleteWatchList(clubId, action.item.id)
                        .onSuccess {
                            getWatchList()
                        }
                        .onError {
                            println("Error deleting movie from watchlist: $it")
                        }
                }
            }

            is ClubAction.OnMoveToWatchList -> {
                viewModelScope.launch {
                    watchListRepository.postWatchList(clubId, action.watchListItem)
                        .onSuccess {
                            watchListRepository.deleteBacklog(clubId, action.watchListItem.id)
                                .onSuccess {
                                    getBacklog()
                                    getWatchList()
                                }
                                .onError {
                                    println("Error moving movie to watchlist: $it")
                                }
                        }
                        .onError {
                            println("Error moving movie to watchlist: $it")
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
                                    getWatchList()
                                    getReviews()
                                }
                                .onError {
                                    println("Error deleting movie from watchlist for review: $it")
                                }
                        }
                        .onError {
                            println("Error posting review: $it")
                        }
                }
            }

            is ClubAction.OnDeleteReview -> {
                viewModelScope.launch {
                    reviewsRepository.deleteReview(clubId, action.reviewId)
                        .onSuccess {
                            getReviews()
                        }
                        .onError {
                            println("Error deleting review: $it")
                        }
                }
            }
        }
    }

    private fun getReviews() = viewModelScope.launch {
        reviewsRepository.getReviews(clubId)
            .onSuccess { reviews ->
                _state.update { it.copy(reviews = AsyncResult.Success(reviews)) }
            }
            .onError {
                _state.update { it.copy(reviews = AsyncResult.Error()) }
            }
    }

    private fun getWatchList() = viewModelScope.launch {
        watchListRepository.getWatchList(clubId)
            .onSuccess { watchList ->
                _state.update { it.copy(watchList = AsyncResult.Success(watchList)) }
            }
            .onError {
                _state.update { it.copy(watchList = AsyncResult.Error()) }
            }
    }

    private fun getBacklog() = viewModelScope.launch {
        watchListRepository.getBacklog(clubId)
            .onSuccess { backlog ->
                _state.update { it.copy(backlog = AsyncResult.Success(backlog)) }
            }
            .onError {
                _state.update { it.copy(backlog = AsyncResult.Error()) }
            }
    }

    private fun getTrendingMovies() = viewModelScope.launch {
        tmdbRepository.getTrendingMovies()
            .onSuccess { trendingMovies ->
                _state.update { it.copy(trendingMovies = AsyncResult.Success(trendingMovies)) }
            }
            .onError {
                _state.update { it.copy(trendingMovies = AsyncResult.Error()) }
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
}

data class ClubState(
    val reviews: AsyncResult<List<Review>> = AsyncResult.Loading,
    val watchList: AsyncResult<List<WatchListItem>> = AsyncResult.Loading,
    val backlog: AsyncResult<List<WatchListItem>> = AsyncResult.Loading,
    val trendingMovies: AsyncResult<List<TmdbMovie>> = AsyncResult.Loading
)
