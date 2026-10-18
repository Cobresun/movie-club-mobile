package cobresun.movieclub.app.core.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class InitialsTest {
    @Test
    fun takesFirstLetterOfFirstTwoWords() {
        assertEquals("MT", initialsOf("Mobile Test Club"))
        assertEquals("C", initialsOf("cobresun"))
    }

    @Test
    fun skipsWordsWithoutLettersLikeEmoji() {
        assertEquals("KB", initialsOf("Katie 🤝 Bnor"))
        assertEquals("NB", initialsOf("🎬 NYC Book Club"))
    }

    @Test
    fun fallsBackForBlankOrSymbolOnlyNames() {
        assertEquals("?", initialsOf("  "))
        assertEquals("?", initialsOf("🍿 🎥"))
    }
}
