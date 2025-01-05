package cobresun.movieclub.app.reviews.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReviewsViewModel(
    private val reviewsRepository: ReviewsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val clubId = savedStateHandle.get<String>("clubId")

    private val _state = MutableStateFlow(ReviewsState())
    val state = _state.asStateFlow()
        .onStart {
            getReviews()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    private fun getReviews() = viewModelScope.launch {
        reviewsRepository.getReviews(clubId!!)
            .onSuccess { reviews ->
                _state.update { it.copy(reviews = reviews) }
            }
            .onError { error ->
                println(error.toString())
            }
    }
}

data class ReviewsState(
    val reviews: List<Review> = emptyList(),
)
