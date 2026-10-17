package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.club.domain.ClubValidation
import cobresun.movieclub.app.club.domain.InviteInfo
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.presentation.components.toUserMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JoinClubState(
    val input: String = "",
    val isLookingUp: Boolean = false,
    val invite: InviteInfo? = null,
    val error: String? = null,
    val isJoining: Boolean = false,
)

sealed interface JoinClubAction {
    data class OnInputChanged(val input: String) : JoinClubAction
    data object OnJoin : JoinClubAction
}

sealed interface JoinClubEvent {
    data class Joined(val clubSlug: String) : JoinClubEvent
}

/** Looks up an invite as soon as a token-shaped value is pasted, then joins on confirm. */
class JoinClubViewModel(
    savedStateHandle: SavedStateHandle,
    private val clubRepository: ClubRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(JoinClubState())
    val state = _state.asStateFlow()

    private val _events = MutableSharedFlow<JoinClubEvent>()
    val events: SharedFlow<JoinClubEvent> = _events.asSharedFlow()

    private var lookupJob: Job? = null

    init {
        savedStateHandle.get<String>("token")?.takeIf { it.isNotBlank() }?.let {
            onAction(JoinClubAction.OnInputChanged(it))
        }
    }

    fun onAction(action: JoinClubAction) {
        when (action) {
            is JoinClubAction.OnInputChanged -> onInput(action.input)
            JoinClubAction.OnJoin -> join()
        }
    }

    private fun onInput(input: String) {
        _state.update { it.copy(input = input, invite = null, error = null) }
        lookupJob?.cancel()
        val token = ClubValidation.parseInviteToken(input) ?: run {
            _state.update { it.copy(isLookingUp = false) }
            return
        }
        lookupJob = viewModelScope.launch {
            delay(350)
            _state.update { it.copy(isLookingUp = true) }
            clubRepository.getInviteInfo(token)
                .onSuccess { info -> _state.update { it.copy(isLookingUp = false, invite = info) } }
                .onError { e ->
                    _state.update {
                        it.copy(
                            isLookingUp = false,
                            // A rejected token reads like the web's join page; connection trouble keeps its retry hint.
                            error = if (e.remote == DataError.Remote.UNKNOWN || e.remote == DataError.Remote.EMAIL_NOT_CONFIRMED) {
                                e.serverMessage?.takeIf { m -> m.isNotBlank() } ?: "The invite token is invalid or expired."
                            } else {
                                e.toUserMessage("find that invite")
                            },
                        )
                    }
                }
        }
    }

    private fun join() {
        val token = ClubValidation.parseInviteToken(_state.value.input) ?: return
        val invite = _state.value.invite ?: return
        viewModelScope.launch {
            _state.update { it.copy(isJoining = true, error = null) }
            clubRepository.joinClub(token)
                .onSuccess {
                    _state.update { it.copy(isJoining = false) }
                    _events.emit(JoinClubEvent.Joined(invite.slug))
                }
                .onError { e ->
                    _state.update { it.copy(isJoining = false, error = e.toUserMessage("join the club")) }
                }
        }
    }
}
