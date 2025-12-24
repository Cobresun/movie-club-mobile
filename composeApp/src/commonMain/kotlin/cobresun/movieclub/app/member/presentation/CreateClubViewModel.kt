package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.MemberRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CreateClubNavigationEvent {
    data class NavigateToClub(val clubId: String) : CreateClubNavigationEvent
}

class CreateClubViewModel(
    private val memberRepository: MemberRepository
) : ViewModel() {
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _state = MutableStateFlow(CreateClubState())
    val state = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<CreateClubNavigationEvent>()
    val navigationEvents: SharedFlow<CreateClubNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            when (val result = memberRepository.getMember()) {
                is Result.Success -> {
                    _state.update { it.copy(currentUserEmail = result.data.email) }
                }
                is Result.Error -> {
                    _errorMessage.update { "Failed to load user. Please try again." }
                }
            }
        }
    }

    fun onAction(action: CreateClubAction) {
        when (action) {
            is CreateClubAction.OnClubNameChanged -> {
                _state.update { it.copy(clubName = action.name) }
            }
            is CreateClubAction.OnCreateClub -> {
                createClub()
            }
            is CreateClubAction.OnClearError -> {
                _errorMessage.update { null }
            }
        }
    }

    private fun createClub() {
        val currentState = _state.value
        if (currentState.clubName.isBlank()) {
            _errorMessage.update { "Please enter a club name" }
            return
        }
        if (currentState.currentUserEmail == null) {
            _errorMessage.update { "Unable to create club. Please try again." }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isCreating = true) }
            memberRepository.createClub(
                name = currentState.clubName,
                memberEmail = currentState.currentUserEmail
            )
                .onSuccess { clubId ->
                    _state.update { it.copy(isCreating = false) }
                    _navigationEvents.emit(CreateClubNavigationEvent.NavigateToClub(clubId))
                }
                .onError {
                    _state.update { it.copy(isCreating = false) }
                    _errorMessage.update { "Failed to create club. Check your connection and try again." }
                }
        }
    }
}

sealed interface CreateClubAction {
    data class OnClubNameChanged(val name: String) : CreateClubAction
    data object OnCreateClub : CreateClubAction
    data object OnClearError : CreateClubAction
}

data class CreateClubState(
    val clubName: String = "",
    val currentUserEmail: String? = null,
    val isCreating: Boolean = false
)
