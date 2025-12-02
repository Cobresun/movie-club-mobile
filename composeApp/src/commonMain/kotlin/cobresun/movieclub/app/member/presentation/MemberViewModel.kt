package cobresun.movieclub.app.member.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.domain.MemberRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MemberViewModel(
    private val memberRepository: MemberRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(MemberState())
    val state = _state.asStateFlow()

    init {
        loadClubs()
    }

    private fun loadClubs() {
        viewModelScope.launch {
            _state.update { it.copy(clubs = AsyncResult.Loading) }
            when (val result = memberRepository.getClubs()) {
                is Result.Success -> {
                    _state.update { it.copy(clubs = AsyncResult.Success(result.data)) }
                }
                is Result.Error -> {
                    _state.update { it.copy(clubs = AsyncResult.Error()) }
                }
            }
        }
    }
}

data class MemberState(
    val clubs: AsyncResult<List<Club>> = AsyncResult.Loading
)
