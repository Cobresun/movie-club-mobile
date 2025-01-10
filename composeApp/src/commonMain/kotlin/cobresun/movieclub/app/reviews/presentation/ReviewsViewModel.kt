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
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _state = MutableStateFlow<ReviewsState>(ReviewsState.Loading)
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
        reviewsRepository.getReviews(clubId)
            .onSuccess { reviews ->
                _state.update { ReviewsState.Loaded(reviews = reviews) }
            }
            .onError { error ->
                _state.update {
                    ReviewsState.Error(errorMessage = error.toString())
                }
            }
    }
}

sealed class ReviewsState {
    data object Loading : ReviewsState()
    data class Loaded(val reviews: List<Review>) : ReviewsState()
    data class Error(val errorMessage: String) : ReviewsState()
}
