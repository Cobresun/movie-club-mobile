package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.StateFlow

enum class ClubTab { REVIEWS, LISTS, STATS, AWARDS }

/**
 * Hosts club-screen chrome state (the selected tab) so it survives
 * configuration changes. Feature state lives in `ListsViewModel` and
 * `ReviewsViewModel`; [ClubScreenRoot] coordinates between them.
 */
class ClubViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /** Slug of the club this screen shows (navigation argument). */
    val clubSlug: String = requireNotNull(savedStateHandle.get<String>("clubSlug"))

    val selectedTab: StateFlow<ClubTab> = savedStateHandle.getStateFlow(KEY_TAB, ClubTab.REVIEWS)

    fun selectTab(tab: ClubTab) {
        savedStateHandle[KEY_TAB] = tab
    }

    companion object {
        /** Saved-state key of the selected tab; other screens may set it to switch tabs on return. */
        const val KEY_TAB = "club_tab"
    }
}
