package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.AccountRepository
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.member.presentation.components.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object PasswordRules {
    const val MIN_LENGTH = 8

    /** Mirrors the web ChangePasswordForm checks; null when the input can be submitted. */
    fun validate(current: String, new: String, confirm: String): String? = when {
        current.isEmpty() -> "Enter your current password"
        new.length < MIN_LENGTH -> "New password must be at least $MIN_LENGTH characters"
        new != confirm -> "Passwords don't match"
        new == current -> "New password must be different from current password"
        else -> null
    }
}

data class ProfileState(
    val member: AsyncResult<Member> = AsyncResult.Loading,
    val isEditingName: Boolean = false,
    val isSavingName: Boolean = false,
    val nameError: String? = null,
    val isUploadingAvatar: Boolean = false,
    val showRemoveAvatarConfirmation: Boolean = false,
    val showChangePassword: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordError: String? = null,
    val message: String? = null,
)

sealed interface ProfileAction {
    data object Retry : ProfileAction
    data object OnEditName : ProfileAction
    data object OnDismissEditName : ProfileAction
    data class OnSaveName(val name: String) : ProfileAction
    data class OnAvatarPicked(val bytes: ByteArray?, val mimeType: String) : ProfileAction
    data object OnRemoveAvatar : ProfileAction
    data object OnConfirmRemoveAvatar : ProfileAction
    data object OnCancelRemoveAvatar : ProfileAction
    data object OnChangePassword : ProfileAction
    data object OnDismissChangePassword : ProfileAction
    data class OnSubmitPassword(val current: String, val new: String, val revokeOthers: Boolean) : ProfileAction
    data object OnClearMessage : ProfileAction
}

class ProfileViewModel(
    private val memberRepository: MemberRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    init {
        load(showLoading = true)
    }

    private fun load(showLoading: Boolean) {
        viewModelScope.launch {
            if (showLoading) _state.update { it.copy(member = AsyncResult.Loading) }
            when (val r = memberRepository.getMember()) {
                is Result.Success -> _state.update { it.copy(member = AsyncResult.Success(r.data)) }
                is Result.Error -> if (showLoading) _state.update {
                    it.copy(member = AsyncResult.Error(IllegalStateException(r.error.toUserMessage("load your profile")), r.error))
                }
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            ProfileAction.Retry -> load(showLoading = true)
            ProfileAction.OnEditName -> _state.update { it.copy(isEditingName = true, nameError = null) }
            ProfileAction.OnDismissEditName -> _state.update { it.copy(isEditingName = false, nameError = null) }
            is ProfileAction.OnSaveName -> saveName(action.name)
            is ProfileAction.OnAvatarPicked -> uploadAvatar(action.bytes, action.mimeType)
            ProfileAction.OnRemoveAvatar -> _state.update { it.copy(showRemoveAvatarConfirmation = true) }
            ProfileAction.OnCancelRemoveAvatar -> _state.update { it.copy(showRemoveAvatarConfirmation = false) }
            ProfileAction.OnConfirmRemoveAvatar -> removeAvatar()
            ProfileAction.OnChangePassword -> _state.update { it.copy(showChangePassword = true, passwordError = null) }
            ProfileAction.OnDismissChangePassword -> _state.update { it.copy(showChangePassword = false, passwordError = null) }
            is ProfileAction.OnSubmitPassword -> changePassword(action.current, action.new, action.revokeOthers)
            ProfileAction.OnClearMessage -> _state.update { it.copy(message = null) }
        }
    }

    private fun saveName(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(isSavingName = true, nameError = null) }
            accountRepository.updateName(name)
                .onSuccess {
                    _state.update { it.copy(isSavingName = false, isEditingName = false, message = "Name updated") }
                    load(showLoading = false)
                }
                .onError { e -> _state.update { it.copy(isSavingName = false, nameError = e.toUserMessage("update your name")) } }
        }
    }

    private fun uploadAvatar(bytes: ByteArray?, mimeType: String) {
        if (bytes == null) return
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true) }
            accountRepository.uploadAvatar(bytes, mimeType)
                .onSuccess {
                    _state.update { it.copy(isUploadingAvatar = false, message = "Photo updated") }
                    load(showLoading = false)
                }
                .onError { e -> _state.update { it.copy(isUploadingAvatar = false, message = e.toUserMessage("upload your photo")) } }
        }
    }

    private fun removeAvatar() {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true, showRemoveAvatarConfirmation = false) }
            accountRepository.removeAvatar()
                .onSuccess {
                    _state.update { it.copy(isUploadingAvatar = false, message = "Photo removed") }
                    load(showLoading = false)
                }
                .onError { e -> _state.update { it.copy(isUploadingAvatar = false, message = e.toUserMessage("remove your photo")) } }
        }
    }

    private fun changePassword(current: String, new: String, revokeOthers: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isChangingPassword = true, passwordError = null) }
            accountRepository.changePassword(current, new, revokeOthers)
                .onSuccess {
                    _state.update { it.copy(isChangingPassword = false, showChangePassword = false, message = "Password changed") }
                }
                .onError { e ->
                    val msg = e.serverMessage?.let { m ->
                        if (m.contains("invalid", true) || m.contains("incorrect", true)) "Current password is incorrect" else m
                    } ?: e.remote.toUserMessage("change your password")
                    _state.update { it.copy(isChangingPassword = false, passwordError = msg) }
                }
        }
    }
}
