package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubFeatures
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.club.domain.ClubValidation
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.platform.ClipboardManager
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.member.presentation.components.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ClubSettingsAction {
    data object Retry : ClubSettingsAction
    data object OnEditName : ClubSettingsAction
    data object OnEditSlug : ClubSettingsAction
    data object OnDismissEdit : ClubSettingsAction
    data class OnSaveName(val name: String) : ClubSettingsAction
    data class OnSaveSlug(val slug: String) : ClubSettingsAction
    data class OnToggleAwards(val enabled: Boolean) : ClubSettingsAction
    data class OnToggleDiscussionQuestions(val enabled: Boolean) : ClubSettingsAction
    data object OnCreateInviteLink : ClubSettingsAction
    data object OnCopyInviteLink : ClubSettingsAction
    data class OnRemoveMember(val member: Member) : ClubSettingsAction
    data object OnConfirmRemoveMember : ClubSettingsAction
    data object OnCancelRemoveMember : ClubSettingsAction
    data object OnLeaveClub : ClubSettingsAction
    data object OnConfirmLeaveClub : ClubSettingsAction
    data object OnCancelLeaveClub : ClubSettingsAction
    data object OnClearMessage : ClubSettingsAction
}

sealed interface ClubSettingsNavigationEvent {
    data object NavigateAfterLeave : ClubSettingsNavigationEvent
    /** Club name or slug changed; the shell should refresh its club list. */
    data class ClubUpdated(val newSlug: String) : ClubSettingsNavigationEvent
}

enum class EditField { NAME, SLUG }

data class ClubSettingsState(
    val clubSlug: String,
    val club: AsyncResult<Club> = AsyncResult.Loading,
    val members: AsyncResult<List<Member>> = AsyncResult.Loading,
    val features: ClubFeatures? = null,
    val currentUserId: String? = null,
    val editing: EditField? = null,
    val isSaving: Boolean = false,
    val editError: String? = null,
    val inviteLink: String? = null,
    val isCreatingInvite: Boolean = false,
    val memberPendingRemoval: Member? = null,
    val isRemovingMember: Boolean = false,
    val showLeaveConfirmation: Boolean = false,
    val isLeavingClub: Boolean = false,
    val message: String? = null,
)

class ClubSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val clubRepository: ClubRepository,
    private val memberRepository: MemberRepository,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ClubSettingsState(clubSlug = requireNotNull(savedStateHandle.get<String>("clubSlug")))
    )
    val state = _state.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ClubSettingsNavigationEvent>()
    val navigationEvents: SharedFlow<ClubSettingsNavigationEvent> = _navigationEvents.asSharedFlow()

    private val slug get() = _state.value.clubSlug

    init {
        load()
    }

    private fun load() {
        _state.update { it.copy(club = AsyncResult.Loading, members = AsyncResult.Loading) }
        viewModelScope.launch {
            val clubs = async { memberRepository.getClubs() }
            val members = async { clubRepository.getMembers(slug) }
            val me = async { memberRepository.getMember() }
            val features = async { clubRepository.getFeatures(slug) }

            when (val r = clubs.await()) {
                is Result.Success -> {
                    val club = r.data.firstOrNull { it.slug == slug }
                    _state.update {
                        it.copy(
                            club = if (club != null) AsyncResult.Success(club)
                            else AsyncResult.Error(IllegalStateException("You're no longer a member of this club."))
                        )
                    }
                }
                is Result.Error -> _state.update {
                    it.copy(club = AsyncResult.Error(
                        IllegalStateException(r.error.toUserMessage("load club settings")), r.error
                    ))
                }
            }
            when (val r = members.await()) {
                is Result.Success -> _state.update { it.copy(members = AsyncResult.Success(r.data)) }
                is Result.Error -> _state.update {
                    it.copy(members = AsyncResult.Error(IllegalStateException(r.error.toUserMessage("load members")), r.error))
                }
            }
            me.await().onSuccess { m -> _state.update { it.copy(currentUserId = m.id) } }
            features.await().onSuccess { f -> _state.update { it.copy(features = f) } }
        }
    }

    fun onAction(action: ClubSettingsAction) {
        when (action) {
            ClubSettingsAction.Retry -> load()
            ClubSettingsAction.OnEditName -> _state.update { it.copy(editing = EditField.NAME, editError = null) }
            ClubSettingsAction.OnEditSlug -> _state.update { it.copy(editing = EditField.SLUG, editError = null) }
            ClubSettingsAction.OnDismissEdit -> _state.update { it.copy(editing = null, editError = null) }
            is ClubSettingsAction.OnSaveName -> saveName(action.name)
            is ClubSettingsAction.OnSaveSlug -> saveSlug(action.slug)
            is ClubSettingsAction.OnToggleAwards -> updateFeatures(awards = action.enabled)
            is ClubSettingsAction.OnToggleDiscussionQuestions -> updateFeatures(discussion = action.enabled)
            ClubSettingsAction.OnCreateInviteLink -> createInvite()
            ClubSettingsAction.OnCopyInviteLink -> copyInvite()
            is ClubSettingsAction.OnRemoveMember -> _state.update { it.copy(memberPendingRemoval = action.member) }
            ClubSettingsAction.OnCancelRemoveMember -> _state.update { it.copy(memberPendingRemoval = null) }
            ClubSettingsAction.OnConfirmRemoveMember -> removeMember()
            ClubSettingsAction.OnLeaveClub -> _state.update { it.copy(showLeaveConfirmation = true) }
            ClubSettingsAction.OnCancelLeaveClub -> _state.update { it.copy(showLeaveConfirmation = false) }
            ClubSettingsAction.OnConfirmLeaveClub -> leave()
            ClubSettingsAction.OnClearMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun updateClub(transform: (Club) -> Club) {
        val current = _state.value.club
        if (current is AsyncResult.Success) {
            _state.update { it.copy(club = AsyncResult.Success(transform(current.data))) }
        }
    }

    private fun saveName(name: String) {
        if (ClubValidation.validateName(name) != null) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, editError = null) }
            clubRepository.updateName(slug, name)
                .onSuccess {
                    updateClub { it.copy(name = name.trim()) }
                    _state.update { it.copy(isSaving = false, editing = null, message = "Club renamed") }
                    _navigationEvents.emit(ClubSettingsNavigationEvent.ClubUpdated(slug))
                }
                .onError { e ->
                    _state.update { it.copy(isSaving = false, editError = e.toUserMessage("rename the club")) }
                }
        }
    }

    private fun saveSlug(newSlug: String) {
        if (ClubValidation.validateSlug(newSlug) != null) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, editError = null) }
            clubRepository.updateSlug(slug, newSlug)
                .onSuccess { saved ->
                    updateClub { it.copy(slug = saved) }
                    _state.update {
                        it.copy(clubSlug = saved, isSaving = false, editing = null, inviteLink = null, message = "Club URL updated")
                    }
                    _navigationEvents.emit(ClubSettingsNavigationEvent.ClubUpdated(saved))
                }
                .onError { e ->
                    _state.update { it.copy(isSaving = false, editError = e.toUserMessage("change the URL")) }
                }
        }
    }

    private fun updateFeatures(awards: Boolean? = null, discussion: Boolean? = null) {
        val previous = _state.value.features ?: ClubFeatures()
        // Optimistic: flip immediately, roll back on failure.
        _state.update {
            it.copy(features = previous.copy(
                awards = awards ?: previous.awards,
                discussionQuestions = discussion ?: previous.discussionQuestions,
            ))
        }
        viewModelScope.launch {
            clubRepository.updateFeatures(slug, awards, discussion)
                .onSuccess { f -> _state.update { it.copy(features = f) } }
                .onError { e ->
                    _state.update { it.copy(features = previous, message = e.toUserMessage("update features")) }
                }
        }
    }

    private fun createInvite() {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingInvite = true) }
            clubRepository.generateInviteToken(slug)
                .onSuccess { token ->
                    _state.update { it.copy(isCreatingInvite = false, inviteLink = ClubValidation.inviteLink(token)) }
                }
                .onError { e ->
                    _state.update { it.copy(isCreatingInvite = false, message = e.toUserMessage("create an invite link")) }
                }
        }
    }

    private fun copyInvite() {
        val link = _state.value.inviteLink ?: return
        viewModelScope.launch {
            try {
                clipboardManager.copyToClipboard(link)
                _state.update { it.copy(message = "Invite link copied") }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Couldn't copy the link") }
            }
        }
    }

    private fun removeMember() {
        val member = _state.value.memberPendingRemoval ?: return
        viewModelScope.launch {
            _state.update { it.copy(isRemovingMember = true) }
            clubRepository.removeMember(slug, member.id)
                .onSuccess {
                    val members = (_state.value.members as? AsyncResult.Success)?.data.orEmpty()
                    _state.update {
                        it.copy(
                            isRemovingMember = false,
                            memberPendingRemoval = null,
                            members = AsyncResult.Success(members.filterNot { m -> m.id == member.id }),
                            message = "${member.name} was removed",
                        )
                    }
                }
                .onError { e ->
                    _state.update {
                        it.copy(isRemovingMember = false, memberPendingRemoval = null, message = e.toUserMessage("remove ${member.name}"))
                    }
                }
        }
    }

    private fun leave() {
        viewModelScope.launch {
            _state.update { it.copy(isLeavingClub = true) }
            clubRepository.leaveClub(slug)
                .onSuccess {
                    _state.update { it.copy(isLeavingClub = false, showLeaveConfirmation = false) }
                    _navigationEvents.emit(ClubSettingsNavigationEvent.NavigateAfterLeave)
                }
                .onError { e ->
                    _state.update {
                        it.copy(isLeavingClub = false, showLeaveConfirmation = false, message = e.toUserMessage("leave the club"))
                    }
                }
        }
    }
}
