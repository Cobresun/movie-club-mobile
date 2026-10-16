package cobresun.movieclub.app.auth.domain

/**
 * Result of a native Sign in with Apple authorization. Apple only returns the
 * user's name on the very first authorization, so it is optional.
 */
data class AppleCredential(
    val identityToken: String,
    val rawNonce: String?,
    val givenName: String? = null,
    val familyName: String? = null,
)
