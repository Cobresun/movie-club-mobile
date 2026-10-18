package cobresun.movieclub.app.core.domain

/** "2h 28m" / "45m" / "2h" (web `formatRuntime`). */
fun formatRuntime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}

/** "250,000" */
fun groupThousands(n: Long): String = n.toString().reversed().chunked(3).joinToString(",").reversed()
