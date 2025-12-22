package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.Member
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
    private val tmdbRepository: TmdbRepository,
    private val memberRepository: cobresun.movieclub.app.member.domain.MemberRepository
) : ViewModel() {
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _state = MutableStateFlow(ClubState())
    val state = _state.asStateFlow()

    init {
        loadCurrentUser()
        loadReviews()
        loadWatchList()
        loadBacklog()
        loadTrendingMovies()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = memberRepository.getMember()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            currentUser = result.data
                        )
                    }
                }

                is Result.Error -> {
                    // Silently fail - user can still view, just not edit
                }
            }
        }
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

    private fun refreshReviews() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingReviews = true) }
            when (val result = reviewsRepository.getReviews(clubId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            reviews = AsyncResult.Success(result.data),
                            isRefreshingReviews = false
                        )
                    }
                }

                is Result.Error -> {
                    _state.update { it.copy(isRefreshingReviews = false) }
                    _errorMessage.update { "Failed to refresh reviews" }
                }
            }
        }
    }

    private fun refreshWatchList() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingWatchList = true) }
            when (val result = watchListRepository.getWatchList(clubId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            watchList = AsyncResult.Success(result.data),
                            isRefreshingWatchList = false
                        )
                    }
                }

                is Result.Error -> {
                    _state.update { it.copy(isRefreshingWatchList = false) }
                    _errorMessage.update { "Failed to refresh watch list" }
                }
            }
        }
    }

    private fun refreshBacklog() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingBacklog = true) }
            when (val result = watchListRepository.getBacklog(clubId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            backlog = AsyncResult.Success(result.data),
                            isRefreshingBacklog = false
                        )
                    }
                }

                is Result.Error -> {
                    _state.update { it.copy(isRefreshingBacklog = false) }
                    _errorMessage.update { "Failed to refresh backlog" }
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

            is ClubAction.OnSetNextWatch -> {
                viewModelScope.launch {
                    watchListRepository.setNextWatch(clubId, action.watchListItem.id)
                        .onSuccess {
                            loadWatchList()
                        }
                        .onError {
                            _errorMessage.update { "Failed to set next watch" }
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

            is ClubAction.OnSubmitScore -> {
                viewModelScope.launch {
                    reviewsRepository.submitScore(
                        clubId = clubId,
                        reviewWorkId = action.reviewWorkId,
                        scoreId = action.scoreId,
                        score = action.scoreValue
                    )
                        .onSuccess {
                            loadReviews()
                        }
                        .onError {
                            _errorMessage.update { "Failed to submit score" }
                        }
                }
            }

            is ClubAction.OnClearError -> {
                _errorMessage.update { null }
            }

            is ClubAction.OnRefreshReviews -> refreshReviews()
            is ClubAction.OnRefreshWatchList -> refreshWatchList()
            is ClubAction.OnRefreshBacklog -> refreshBacklog()
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
    data class OnSetNextWatch(val watchListItem: WatchListItem) : ClubAction
    data class OnDeleteReview(val reviewId: String) : ClubAction
    data class OnSubmitScore(
        val reviewWorkId: String,
        val scoreId: String?,
        val scoreValue: Double
    ) : ClubAction

    data object OnClearError : ClubAction
    data object OnRefreshReviews : ClubAction
    data object OnRefreshWatchList : ClubAction
    data object OnRefreshBacklog : ClubAction
}

data class ClubState(
    val reviews: AsyncResult<List<Review>> = AsyncResult.Loading,
    val watchList: AsyncResult<List<WatchListItem>> = AsyncResult.Loading,
    val backlog: AsyncResult<List<WatchListItem>> = AsyncResult.Loading,
    val trendingMovies: AsyncResult<List<TmdbMovie>> = AsyncResult.Loading,
    val currentUser: Member? = null,
    val isRefreshingReviews: Boolean = false,
    val isRefreshingWatchList: Boolean = false,
    val isRefreshingBacklog: Boolean = false
)
