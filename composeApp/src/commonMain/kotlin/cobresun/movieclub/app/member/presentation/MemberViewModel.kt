package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemberViewModel(
    private val memberRepository: MemberRepository,
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _state = MutableStateFlow(MemberState())
    val state = _state.asStateFlow()
        .onStart {
            getClubs()
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            _state.value
        )

    private fun getClubs() = viewModelScope.launch {
        authRepository.userAccessToken.collect { accessToken ->
            memberRepository.getClubs(accessToken!!)
                .onSuccess { clubs ->
                    _state.value = _state.value.copy(clubs = AsyncResult.Success(clubs))
                }
                .onError { error ->
                    _state.value = _state.value.copy(clubs = AsyncResult.Error())
                }
        }
    }
}

data class MemberState(
    val clubs: AsyncResult<List<Club>> = AsyncResult.Loading
)
