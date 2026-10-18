package cobresun.movieclub.app.core.presentation

/**
 * Up to two initials for an avatar fallback: the first letter or digit of each of
 * the first two words that have one. Words without any (an emoji, "&") are skipped,
 * so "Katie 🤝 Bnor" gives "KB" rather than half of the emoji's surrogate pair.
 */
fun initialsOf(name: String): String =
    name.trim().split(Regex("\\s+"))
        .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
        .take(2)
        .joinToString("") { it.uppercase() }
        .ifEmpty { "?" }
