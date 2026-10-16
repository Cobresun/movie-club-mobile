package cobresun.movieclub.app.club.domain

import cobresun.movieclub.app.core.domain.Constants.BASE_URL

/** Client-side mirror of the server's slug rules (web/netlify/functions/utils/slug.ts). */
object ClubValidation {
    const val SLUG_MIN = 3
    const val SLUG_MAX = 50
    const val NAME_MAX = 100
    private val SLUG_PATTERN = Regex("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$")
    private val RESERVED = setOf(
        "new", "create", "join", "settings", "api", "share", "admin", "invite",
        "clubs", "member", "auth", "login", "signup", "logout", "profile",
    )

    /** Returns a user-facing error, or null when [slug] is acceptable. */
    fun validateSlug(slug: String): String? = when {
        slug.length < SLUG_MIN -> "URL must be at least $SLUG_MIN characters"
        slug.length > SLUG_MAX -> "URL must be at most $SLUG_MAX characters"
        !SLUG_PATTERN.matches(slug) ->
            "Use lowercase letters, numbers and hyphens; can't start or end with a hyphen"
        slug in RESERVED -> "That URL is reserved"
        else -> null
    }

    fun validateName(name: String): String? = when {
        name.isBlank() -> "Name can't be empty"
        name.trim().length > NAME_MAX -> "Name must be at most $NAME_MAX characters"
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
