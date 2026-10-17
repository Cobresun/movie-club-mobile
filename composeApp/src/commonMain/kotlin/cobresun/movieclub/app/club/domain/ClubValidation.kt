package cobresun.movieclub.app.club.domain

import cobresun.movieclub.app.core.domain.Constants.BASE_URL

/** Client-side club name, link and invite rules, matching the web. */
object ClubValidation {
    const val MIN_SLUG_LENGTH = 3
    const val MAX_SLUG_LENGTH = 50
    const val MAX_NAME_LENGTH = 100
    private val SLUG_PATTERN = Regex("^[a-z0-9-]+$")
    private val ALL_DIGITS = Regex("^[0-9]+$")

    /**
     * Returns a user-facing error, or null when [slug] is acceptable. Same rules and
     * copy as the web's club-link editor; the server has the final say (reserved or
     * taken links come back with its own message).
     */
    fun validateSlug(slug: String): String? = when {
        slug.length !in MIN_SLUG_LENGTH..MAX_SLUG_LENGTH -> "Link must be $MIN_SLUG_LENGTH-$MAX_SLUG_LENGTH characters"
        !SLUG_PATTERN.matches(slug) -> "Only lowercase letters, numbers, and hyphens allowed"
        ALL_DIGITS.matches(slug) -> "Link cannot be all numbers"
        else -> null
    }

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Name cannot be empty"
        name.trim().length > MAX_NAME_LENGTH -> "Name must be at most $MAX_NAME_LENGTH characters"
        else -> null
    }

    /** Lower-cases and replaces characters that can never appear in a slug, while typing. */
    fun sanitizeSlugInput(input: String): String =
        input.lowercase().replace(' ', '-').filter { it in 'a'..'z' || it in '0'..'9' || it == '-' }

    /**
     * Extracts an invite token from a pasted invite link (https or movieclub://) or a bare token.
     * Returns null when nothing token-like is present.
     */
    fun parseInviteToken(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        val marker = "join-club/"
        val raw = if (marker in trimmed) trimmed.substringAfter(marker) else trimmed
        val token = raw.substringBefore('?').substringBefore('#').trimEnd('/')
        if (token.isEmpty() || token.any { it.isWhitespace() || it == '/' }) return null
        return token
    }

    fun inviteLink(token: String): String = "$BASE_URL/join-club/$token"
}
