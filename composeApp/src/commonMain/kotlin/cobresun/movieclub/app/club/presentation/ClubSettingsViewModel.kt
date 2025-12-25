package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.member.domain.Member
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ClubSettingsAction {
    data object OnCopyInviteLink : ClubSettingsAction
    data object OnLeaveClub : ClubSettingsAction
    data object OnConfirmLeaveClub : ClubSettingsAction
    data object OnCancelLeaveClub : ClubSettingsAction
    data object OnClearError : ClubSettingsAction
}

sealed interface ClubSettingsNavigationEvent {
    data object NavigateAfterLeave : ClubSettingsNavigationEvent
}

data class ClubSettingsState(
    val members: AsyncResult<List<Member>> = AsyncResult.Loading,
    val inviteLink: String = "",
    val hasCopiedLink: Boolean = false,
    val isLeavingClub: Boolean = false,
    val showLeaveConfirmation: Boolean = false
)

class ClubSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val clubRepository: ClubRepository,
    private val clipboardManager: ClipboardManager
) : ViewModel() {
    private val clubId = requireNotNull(savedStateHandle.get<String>("clubId"))

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private val _state = MutableStateFlow(ClubSettingsState())
    val state = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ClubSettingsNavigationEvent>()
    val navigationEvents: SharedFlow<ClubSettingsNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        loadMembers()
        loadInviteLink()
    }

    private fun loadMembers() {
        viewModelScope.launch {
            _state.update { it.copy(members = AsyncResult.Loading) }
            when (val result = clubRepository.getMembers(clubId)) {
                is Result.Success -> {
                    _state.update { it.copy(members = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(members = AsyncResult.Error()) }
                    _errorMessage.update { "Failed to load members" }
                }
            }
        }
    }

    private fun loadInviteLink() {
        viewModelScope.launch {
            when (val result = clubRepository.generateInviteToken(clubId)) {
                is Result.Success -> {
                    val inviteLink = "$BASE_URL/join-club/${result.data}"
                    _state.update { it.copy(inviteLink = inviteLink) }
                }
                is Result.Error -> {
                    _errorMessage.update { "Failed to generate invite link" }
                }
            }
        }
    }

    fun onAction(action: ClubSettingsAction) {
        when (action) {
            is ClubSettingsAction.OnCopyInviteLink -> {
                viewModelScope.launch {
                    try {
                        clipboardManager.copyToClipboard(_state.value.inviteLink)
                        _state.update { it.copy(hasCopiedLink = true) }
                        delay(2000)
                        _state.update { it.copy(hasCopiedLink = false) }
                    } catch (e: Exception) {
                        _errorMessage.update { "Failed to copy link" }
                    }
                }
            }
            is ClubSettingsAction.OnLeaveClub -> {
                _state.update { it.copy(showLeaveConfirmation = true) }
            }
            is ClubSettingsAction.OnCancelLeaveClub -> {
                _state.update { it.copy(showLeaveConfirmation = false) }
            }
            is ClubSettingsAction.OnConfirmLeaveClub -> {
                viewModelScope.launch {
                    _state.update {
                        it.copy(
                            showLeaveConfirmation = false,
                            isLeavingClub = true
                        )
                    }

                    clubRepository.leaveClub(clubId)
                        .onSuccess {
                            _state.update { it.copy(isLeavingClub = false) }
                            _navigationEvents.emit(ClubSettingsNavigationEvent.NavigateAfterLeave)
                        }
                        .onError {
                            _state.update { it.copy(isLeavingClub = false) }
                            _errorMessage.update {
                                "Failed to leave club. Check your connection and try again."
                            }
                        }
                }
            }
            is ClubSettingsAction.OnClearError -> {
                _errorMessage.update { null }
            }
        }
    }
}
