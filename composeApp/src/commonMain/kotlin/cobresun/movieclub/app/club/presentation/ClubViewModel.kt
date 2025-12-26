package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Constants
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.reviews.domain.NewReviewItem
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.tmdb.domain.TmdbMovie
import cobresun.movieclub.app.tmdb.domain.TmdbRepository
import cobresun.movieclub.app.watchlist.domain.WatchListItem
import cobresun.movieclub.app.watchlist.domain.WatchListRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ClubNavigationEvent {
    data object NavigateToReviewsTab : ClubNavigationEvent
    data object NavigateToWatchListTab : ClubNavigationEvent
}

class ClubViewModel(
    savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
    private val watchListRepository: WatchListRepository,
    private val tmdbRepository: TmdbRepository,
    private val memberRepository: MemberRepository,
    private val clubRepository: ClubRepository,
    private val clipboardManager: ClipboardManager
) : ViewModel() {
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage = _successMessage.asStateFlow()

    private val _state = MutableStateFlow(ClubState())
    val state = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ClubNavigationEvent>()
    val navigationEvents: SharedFlow<ClubNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        loadCurrentUser()
        loadReviews()
        loadWatchList()
        loadBacklog()
        loadTrendingMovies()
        loadClubMembers()
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
                    val sortedReviews = applySortToReviews(
                        result.data,
                        _state.value.reviewSortState
                    )
                    _state.update { it.copy(reviews = AsyncResult.Success(sortedReviews)) }
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

    private fun loadClubMembers() {
        viewModelScope.launch {
            _state.update { it.copy(clubMembers = AsyncResult.Loading) }
            when (val result = clubRepository.getMembers(clubId)) {
                is Result.Success -> {
                    _state.update { it.copy(clubMembers = AsyncResult.Success(result.data)) }
                }

                is Result.Error -> {
                    _state.update { it.copy(clubMembers = AsyncResult.Error()) }
                    // Silently fail - sorting by members just won't be available
                }
            }
        }
    }

    /**
     * Applies the current sort state to a list of reviews.
     * Returns reviews in sorted order, or original order if sort is None.
     */
    private fun applySortToReviews(
        reviews: List<Review>,
        sortState: ReviewSortState
    ): List<Review> {
        if (sortState.option == ReviewSortOption.None) {
            return reviews
        }

        val comparator = when (val option = sortState.option) {
            ReviewSortOption.None -> return reviews // Early return

            ReviewSortOption.DateReviewed -> compareBy<Review> { review ->
                // Parse ISO date string for comparison
                review.createdDate
            }

            ReviewSortOption.AverageScore -> compareBy { review ->
                calculateAverageScore(review)
            }

            is ReviewSortOption.MemberScore -> compareBy { review ->
                // Get score for specific member, null if not found
                review.scores[option.user]?.value
            }
        }

        return if (sortState.descending) {
            reviews.sortedWith(comparator.reversed())
        } else {
            reviews.sortedWith(comparator)
        }
    }

    /**
     * Calculates average score for a review.
     * Returns null if no scores exist (will be pushed to end in comparisons).
     */
    private fun calculateAverageScore(review: Review): Double? {
        if (review.scores.isEmpty()) return null
        return review.scores.values.map { it.value }.average()
    }

    private fun refreshReviews() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshingReviews = true) }
            when (val result = reviewsRepository.getReviews(clubId)) {
                is Result.Success -> {
                    val sortedReviews = applySortToReviews(
                        result.data,
                        _state.value.reviewSortState
                    )
                    _state.update {
                        it.copy(
                            reviews = AsyncResult.Success(sortedReviews),
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

    private fun refreshBacklogAfterAdd() {
        viewModelScope.launch {
            when (val result = watchListRepository.getBacklog(clubId)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            backlog = AsyncResult.Success(result.data),
                            isAddingToBacklog = false
                        )
                    }
                }

                is Result.Error -> {
                    // Keep existing list visible, just clear the loading flag
                    _state.update { it.copy(isAddingToBacklog = false) }
                    _errorMessage.update { "Movie added but failed to refresh list" }
                }
            }
        }
    }

    fun onAction(action: ClubAction) {
        when (action) {
            is ClubAction.OnAddMovieToWatchList -> {
                viewModelScope.launch {
                    _state.update { it.copy(isAddingToBacklog = true) }
                    watchListRepository.postWatchListFromMovie(clubId, action.movie)
                        .onSuccess {
                            refreshBacklogAfterAdd()
                        }
                        .onError {
                            _state.update { it.copy(isAddingToBacklog = false) }
                            _errorMessage.update { "Failed to add movie" }
                        }
                }
            }

            is ClubAction.OnAddMovieToBacklog -> {
                viewModelScope.launch {
                    _state.update { it.copy(isAddingToBacklog = true) }
                    watchListRepository.postBacklog(clubId, action.movie)
                        .onSuccess {
                            refreshBacklogAfterAdd()
                        }
                        .onError {
                            _state.update { it.copy(isAddingToBacklog = false) }
                            _errorMessage.update { "Failed to add movie to backlog" }
                        }
                }
            }

            is ClubAction.OnDeleteBacklogItem -> {
                viewModelScope.launch {
                    watchListRepository.deleteBacklog(clubId, action.item.id)
                        .onSuccess {
                            refreshBacklog()
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
                            refreshWatchList()
                        }
                        .onError {
                            _errorMessage.update { "Failed to delete item" }
                        }
                }
            }

            is ClubAction.OnMoveToWatchList -> {
                viewModelScope.launch {
                    _navigationEvents.emit(ClubNavigationEvent.NavigateToWatchListTab)
                    watchListRepository.postWatchList(clubId, action.watchListItem)
                        .onSuccess {
                            watchListRepository.deleteBacklog(clubId, action.watchListItem.id)
                                .onSuccess {
                                    refreshWatchList()
                                    refreshBacklog()
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
                                    refreshReviews()
                                    refreshWatchList()
                                    _navigationEvents.emit(ClubNavigationEvent.NavigateToReviewsTab)
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
                            refreshWatchList()
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
                            refreshReviews()
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
                            refreshReviews()
                        }
                        .onError {
                            _errorMessage.update { "Failed to submit score" }
                        }
                }
            }

            is ClubAction.OnShareReview -> {
                viewModelScope.launch {
                    try {
                        val shareUrl = "${Constants.BASE_URL}/share/club/$clubId/review/${action.reviewId}"
                        clipboardManager.copyToClipboard(shareUrl)
                        _successMessage.update { "Link copied to clipboard" }
                    } catch (e: Exception) {
                        _errorMessage.update { "Failed to copy share link" }
                    }
                }
            }

            is ClubAction.OnSortReviews -> {
                val newSortState = ReviewSortState(
                    option = action.sortOption,
                    descending = action.sortOption.defaultDescending
                )
                _state.update { it.copy(reviewSortState = newSortState) }

                // Re-apply sort to existing reviews
                val currentReviews = _state.value.reviews
                if (currentReviews is AsyncResult.Success) {
                    val sortedReviews = applySortToReviews(
                        currentReviews.data,
                        newSortState
                    )
                    _state.update { it.copy(reviews = AsyncResult.Success(sortedReviews)) }
                }
            }

            is ClubAction.OnToggleSortDirection -> {
                val currentSort = _state.value.reviewSortState
                val newSortState = currentSort.copy(descending = !currentSort.descending)
                _state.update { it.copy(reviewSortState = newSortState) }

                // Re-apply sort with new direction
                val currentReviews = _state.value.reviews
                if (currentReviews is AsyncResult.Success) {
                    val sortedReviews = applySortToReviews(
                        currentReviews.data,
                        newSortState
                    )
                    _state.update { it.copy(reviews = AsyncResult.Success(sortedReviews)) }
                }
            }

            is ClubAction.OnClearSort -> {
                onAction(ClubAction.OnSortReviews(ReviewSortOption.None))
            }

            is ClubAction.OnClearError -> {
                _errorMessage.update { null }
            }

            is ClubAction.OnClearSuccess -> {
                _successMessage.update { null }
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
    data class OnShareReview(val reviewId: String) : ClubAction

    data class OnSortReviews(val sortOption: ReviewSortOption) : ClubAction
    data object OnToggleSortDirection : ClubAction
    data object OnClearSort : ClubAction

    data object OnClearError : ClubAction
    data object OnClearSuccess : ClubAction
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
    val isRefreshingBacklog: Boolean = false,
    val isAddingToBacklog: Boolean = false,
    val reviewSortState: ReviewSortState = ReviewSortState(),
    val clubMembers: AsyncResult<List<Member>> = AsyncResult.Loading
)
