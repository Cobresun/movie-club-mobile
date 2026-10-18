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
import cobresun.movieclub.app.core.presentation.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Web profile name limit. */
const val MAX_NAME_LENGTH = 100

object PasswordRules {
    const val MIN_LENGTH = 8

    /** The web ChangePasswordForm checks; null when the input can be submitted. */
    fun validate(current: String, new: String): String? = when {
        new.length < MIN_LENGTH -> "New password must be at least $MIN_LENGTH characters."
        new == current -> "New password must be different from current password."
        else -> null
    }
}

data class ProfileState(
    val member: AsyncResult<Member> = AsyncResult.Loading,
    val isSavingName: Boolean = false,
    val nameError: String? = null,
    val isUploadingAvatar: Boolean = false,
    val isChangingPassword: Boolean = false,
    val passwordError: String? = null,
    /** Bumped on each successful change, so the form clears its fields. */
    val passwordChanges: Int = 0,
    val message: String? = null,
)

sealed interface ProfileAction {
    data object Retry : ProfileAction
    /** The name field changed: drop its error. */
    data object OnNameEdited : ProfileAction
    data class OnSaveName(val name: String) : ProfileAction
    data class OnAvatarPicked(val bytes: ByteArray?, val mimeType: String) : ProfileAction
    data object OnRemoveAvatar : ProfileAction
    /** A password field changed: drop the form error. */
    data object OnPasswordEdited : ProfileAction
    data class OnSubmitPassword(val current: String, val new: String, val revokeOthers: Boolean) : ProfileAction
    data object OnClearMessage : ProfileAction
    /** A picked photo couldn't be decoded or the crop couldn't be encoded. */
    data class OnPhotoFailed(val message: String) : ProfileAction
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
            ProfileAction.OnNameEdited -> _state.update { it.copy(nameError = null) }
            is ProfileAction.OnSaveName -> saveName(action.name)
            is ProfileAction.OnAvatarPicked -> uploadAvatar(action.bytes, action.mimeType)
            // Web removes straight away: the photo can simply be added again.
            ProfileAction.OnRemoveAvatar -> removeAvatar()
            ProfileAction.OnPasswordEdited -> _state.update { it.copy(passwordError = null) }
            is ProfileAction.OnSubmitPassword -> changePassword(action.current, action.new, action.revokeOthers)
            ProfileAction.OnClearMessage -> _state.update { it.copy(message = null) }
            is ProfileAction.OnPhotoFailed -> _state.update { it.copy(message = action.message) }
        }
    }

    private fun saveName(input: String) {
        val name = input.trim()
        val invalid = when {
            name.isEmpty() -> "Name cannot be empty"
            name.length > MAX_NAME_LENGTH -> "Name is too long (max $MAX_NAME_LENGTH characters)"
            else -> null
        }
        if (invalid != null) {
            _state.update { it.copy(nameError = invalid) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSavingName = true, nameError = null) }
            accountRepository.updateName(name)
                .onSuccess {
                    _state.update { it.copy(isSavingName = false, message = "Name updated successfully") }
                    load(showLoading = false)
                }
                .onError { e -> _state.update { it.copy(isSavingName = false, nameError = e.serverMessage?.takeIf { m -> m.isNotBlank() } ?: "Failed to update name") } }
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
                .onError { _ -> _state.update { it.copy(isUploadingAvatar = false, message = "Couldn't update your photo") } }
        }
    }

    private fun removeAvatar() {
        viewModelScope.launch {
            _state.update { it.copy(isUploadingAvatar = true) }
            accountRepository.removeAvatar()
                .onSuccess {
                    _state.update { it.copy(isUploadingAvatar = false, message = "Photo removed") }
                    load(showLoading = false)
                }
                .onError { e -> _state.update { it.copy(isUploadingAvatar = false, message = e.toUserMessage("remove your photo")) } }
        }
    }

    private fun changePassword(current: String, new: String, revokeOthers: Boolean) {
        PasswordRules.validate(current, new)?.let { error ->
            _state.update { it.copy(passwordError = error) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isChangingPassword = true, passwordError = null) }
            accountRepository.changePassword(current, new, revokeOthers)
                .onSuccess {
                    _state.update {
                        it.copy(isChangingPassword = false, passwordChanges = it.passwordChanges + 1, message = "Password changed successfully")
                    }
                }
                .onError { e ->
                    val msg = e.serverMessage?.let { m ->
                        if (m.contains("invalid", true) || m.contains("incorrect", true)) "Current password is incorrect." else m
                    } ?: "Failed to change password. Please try again."
                    _state.update { it.copy(isChangingPassword = false, passwordError = msg) }
                }
        }
    }
}
