package cobresun.movieclub.app.reviews.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.reviews.domain.ReviewFact
import cobresun.movieclub.app.reviews.domain.ReviewFacts
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.reviews.domain.SharedReview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SharedReviewState(
    val review: AsyncResult<SharedReview> = AsyncResult.Loading,
    val fact: ReviewFact? = null,
)

/** A shared review link for a club you're not in: the public page, read-only. */
class SharedReviewViewModel(
    savedStateHandle: SavedStateHandle,
    private val reviewsRepository: ReviewsRepository,
) : ViewModel() {
    private val clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug"))
    private val workId = requireNotNull(savedStateHandle.get<String>("workId"))

    private val _state = MutableStateFlow(SharedReviewState())
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(review = AsyncResult.Loading) }
            val result = reviewsRepository.getSharedReview(clubSlug, workId)
            _state.update {
                it.copy(
                    review = when (result) {
                        is Result.Success -> AsyncResult.Success(result.data)
                        is Result.Error -> AsyncResult.Error(dataError = result.error)
                    }
                )
            }
        }
        // The reviews list is public too, so the page shows the same spotlight fact the club sees.
        viewModelScope.launch {
            reviewsRepository.getReviews(clubSlug).onSuccess { reviews ->
                _state.update { it.copy(fact = ReviewFacts.compute(reviews, workId)) }
            }
        }
    }
}
