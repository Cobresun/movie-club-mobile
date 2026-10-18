package cobresun.movieclub.app.club.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.LastClubStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ClubTab { REVIEWS, LISTS, STATS, AWARDS }

/**
 * Hosts club-screen chrome state (the selected tab) so it survives
 * configuration changes. Feature state lives in `ListsViewModel` and
 * `ReviewsViewModel`; [ClubScreenRoot] coordinates between them.
 */
class ClubViewModel(
    /** Slug of the club this screen shows. */
    val clubSlug: String,
    /** The tab the route names (switching clubs keeps the section); null for the remembered one. */
    initialTab: ClubTab?,
    private val savedStateHandle: SavedStateHandle,
    private val lastClubStore: LastClubStore,
) : ViewModel() {
    init {
        // Before anything reads the tab: `getStateFlow` stores its default in the handle.
        // A restored tab wins, then the one the route names.
        if (initialTab != null && !savedStateHandle.contains(KEY_TAB)) savedStateHandle[KEY_TAB] = initialTab
    }

    private val _tabResolved = MutableStateFlow(savedStateHandle.contains(KEY_TAB))

    /** False until the tab last used in this club has been restored (a few ms on open). */
    val tabResolved: StateFlow<Boolean> = _tabResolved.asStateFlow()

    val selectedTab: StateFlow<ClubTab> = savedStateHandle.getStateFlow(KEY_TAB, ClubTab.REVIEWS)

    init {
        if (!_tabResolved.value) {
            viewModelScope.launch {
                val stored = lastClubStore.getSection(clubSlug)
                    ?.let { name -> ClubTab.entries.firstOrNull { it.name == name } }
                // A tab chosen meanwhile (e.g. by a deep link) wins over the remembered one.
                if (stored != null && !_tabResolved.value) savedStateHandle[KEY_TAB] = stored
                _tabResolved.value = true
            }
        }
    }

    fun selectTab(tab: ClubTab) {
        savedStateHandle[KEY_TAB] = tab
        _tabResolved.value = true
        viewModelScope.launch { lastClubStore.setSection(clubSlug, tab.name) }
    }

    companion object {
        /** Saved-state key of the selected tab. */
        const val KEY_TAB = "club_tab"
    }
}
