package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.member.domain.Member
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ClubSettingsAction {
    data object OnCopyInviteLink : ClubSettingsAction
    data object OnLeaveClub : ClubSettingsAction
    data object OnClearError : ClubSettingsAction
}

data class ClubSettingsState(
    val members: AsyncResult<List<Member>> = AsyncResult.Loading,
    val inviteLink: String = "",
    val hasCopiedLink: Boolean = false,
    val isLeavingClub: Boolean = false,
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
                _errorMessage.update { "Leave club functionality not implemented yet" }
            }
            is ClubSettingsAction.OnClearError -> {
                _errorMessage.update { null }
            }
        }
    }
}
