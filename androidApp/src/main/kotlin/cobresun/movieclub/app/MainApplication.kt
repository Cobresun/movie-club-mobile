package cobresun.movieclub.app

import android.app.Application
import android.content.pm.ApplicationInfo
import cobresun.movieclub.app.core.platform.AppLog
import cobresun.movieclub.app.di.initKoin
import org.koin.android.ext.koin.androidContext

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLog.debugEnabled = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        initKoin {
            androidContext(this@MainApplication)
        }
    }
}
