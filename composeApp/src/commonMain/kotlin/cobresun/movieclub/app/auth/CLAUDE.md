### Authentication Implementation

The app uses **BetterAuth** for authentication with HTTP cookie-based sessions:

**Backend API Endpoints:**
- `POST /api/auth/sign-up/email` - Register new user (requires email verification)
- `POST /api/auth/sign-in/email` - Sign in with email/password
- `GET /api/auth/get-session` - Get current user session
- `POST /api/auth/sign-out` - Sign out and clear session
- `POST /api/auth/send-verification-email` - Resend verification email

**Session Management:**
- Server sets HTTP-only session cookies on successful authentication
- Ktor HttpCookies plugin automatically includes cookies in all requests
- `SessionCookieStorage` persists cookies to DataStore for cross-app-launch persistence
- Cookie rules live in `core/data/CookieJar.kt` (pure, unit-tested): `Max-Age` is stored as an absolute expiry, `Max-Age=0`/past `Expires` delete, and cookies only go back to their own host
- Launch: a stored session cookie is the signed-in hint (`AuthRepository.hasStoredSession`), so the club shell paints at once while `AuthViewModel` confirms the session in the background. Offline/server errors keep the user signed in and retry; only the server answering "no session" signs out
- Platform-specific DataStore paths:
  - Android: `androidx.datastore.preferences` with "session_cookies" name
  - iOS: `~/Documents/session_cookies.preferences_pb`

**Email Verification:**
- Users must verify email via link before first login
- Unverified login returns `DataError.Remote.EMAIL_NOT_CONFIRMED` error
- UI prompts user to check email and provides resend option

