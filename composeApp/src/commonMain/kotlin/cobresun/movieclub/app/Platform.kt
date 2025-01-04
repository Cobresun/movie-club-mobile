package cobresun.movieclub.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform