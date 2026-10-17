package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubValidation
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.member.presentation.components.toUserMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CreateClubNavigationEvent {
    data class NavigateToClub(val clubSlug: String, val clubName: String, val type: ClubType) : CreateClubNavigationEvent
    data class NavigateToJoin(val token: String) : CreateClubNavigationEvent
}

class CreateClubViewModel(
    private val memberRepository: MemberRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CreateClubState())
    val state = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<CreateClubNavigationEvent>()
    val navigationEvents: SharedFlow<CreateClubNavigationEvent> = _navigationEvents.asSharedFlow()

    fun onAction(action: CreateClubAction) {
        when (action) {
            is CreateClubAction.OnClubNameChanged -> _state.update { it.copy(clubName = action.name, error = null) }
            is CreateClubAction.OnTypeChanged -> _state.update { it.copy(type = action.type) }
            is CreateClubAction.OnCreateClub -> createClub()
            is CreateClubAction.OnClearError -> _state.update { it.copy(error = null) }
            is CreateClubAction.OnInviteChanged -> _state.update { it.copy(invite = action.value, inviteError = null) }
            is CreateClubAction.OnJoin -> join()
        }
    }

    private fun createClub() {
        val current = _state.value
        if (current.isCreating) return
        if (current.clubName.isBlank()) {
            _state.update { it.copy(error = "Club name is required") }
            return
        }
        ClubValidation.validateName(current.clubName)?.let { msg ->
            _state.update { it.copy(error = msg) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            // The creator is passed as the first member, which the server makes admin.
            val email = when (val me = memberRepository.getMember()) {
                is Result.Success -> me.data.email
                is Result.Error -> {
                    _state.update { it.copy(isCreating = false, error = me.error.toUserMessage("create the club")) }
                    return@launch
                }
            }
            memberRepository.createClub(name = current.clubName.trim(), memberEmail = email, type = current.type)
                .onSuccess { slug ->
                    _state.update { it.copy(isCreating = false) }
                    _navigationEvents.emit(CreateClubNavigationEvent.NavigateToClub(slug, current.clubName.trim(), current.type))
                }
                .onError { e ->
                    _state.update { it.copy(isCreating = false, error = e.toUserMessage("create the club")) }
                }
        }
    }

    private fun join() {
        val token = ClubValidation.parseInviteToken(_state.value.invite)
        if (token == null) {
            _state.update { it.copy(inviteError = "That doesn't look like an invite link.") }
            return
        }
        _state.update { it.copy(inviteError = null) }
        viewModelScope.launch { _navigationEvents.emit(CreateClubNavigationEvent.NavigateToJoin(token)) }
    }
}

sealed interface CreateClubAction {
    data class OnClubNameChanged(val name: String) : CreateClubAction
    data class OnTypeChanged(val type: ClubType) : CreateClubAction
    data object OnCreateClub : CreateClubAction
    data object OnClearError : CreateClubAction
    data class OnInviteChanged(val value: String) : CreateClubAction
    data object OnJoin : CreateClubAction
}

data class CreateClubState(
    val clubName: String = "",
    val type: ClubType = ClubType.MOVIE,
    val isCreating: Boolean = false,
    val error: String? = null,
    /** A pasted invite link (or bare token) to join instead. */
    val invite: String = "",
    val inviteError: String? = null,
)
