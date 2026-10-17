package cobresun.movieclub.app.core.presentation

import androidx.compose.runtime.Composable

/**
 * Whether the user asked the system to cut down on motion (iOS "Reduce
 * Motion", Android "Remove animations"). Decorative animations should be
 * skipped or replaced with an instant change when this is true.
 */
@Composable
expect fun rememberReducedMotion(): Boolean
