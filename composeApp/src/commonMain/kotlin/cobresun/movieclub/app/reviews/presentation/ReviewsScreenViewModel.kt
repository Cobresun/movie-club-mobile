package cobresun.movieclub.app.reviews.presentation

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

class ReviewsScreenViewModel(
    private val reviewsRepository: ReviewsRepository
) : ViewModel() {
    private val _state = MutableStateFlow<List<Review>>(emptyList())
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
        reviewsRepository.getReviews(COBRESUN_CLUB_ID)
            .onSuccess { reviews ->
                _state.update { reviews }
            }
            .onError { error ->
                println(error.toString())
            }
    }

    companion object {
        private const val COBRESUN_CLUB_ID = "946516463182315521"
    }
}
