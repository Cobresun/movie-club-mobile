package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.club.domain.ClubValidation
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.platform.ClipboardManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ClubInviteState(
    val clubSlug: String,
    val clubName: String,
    val clubType: ClubType,
    val inviteLink: AsyncResult<String> = AsyncResult.Loading,
)

/** The handoff after creating a club (web `ClubInviteView`): get the invite link out. */
class ClubInviteViewModel(
    clubSlug: String,
    clubName: String,
    /** A `ClubType.value`. */
    clubType: String,
    private val clubRepository: ClubRepository,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ClubInviteState(
            clubSlug = clubSlug,
            clubName = clubName.ifBlank { "Your club" },
            clubType = ClubType.from(clubType),
        )
    )
    val state = _state.asStateFlow()

    init {
        loadLink()
    }

    fun loadLink() {
        _state.update { it.copy(inviteLink = AsyncResult.Loading) }
        viewModelScope.launch {
            val link = when (val r = clubRepository.generateInviteToken(_state.value.clubSlug)) {
                is Result.Success -> AsyncResult.Success(ClubValidation.inviteLink(r.data))
                is Result.Error -> AsyncResult.Error(dataError = r.error)
            }
            _state.update { it.copy(inviteLink = link) }
        }
    }

    /** Returns whether the link reached the clipboard. */
    suspend fun copyLink(): Boolean {
        val link = (_state.value.inviteLink as? AsyncResult.Success)?.data ?: return false
        return runCatching { clipboardManager.copyToClipboard(link) }.isSuccess
    }
}
