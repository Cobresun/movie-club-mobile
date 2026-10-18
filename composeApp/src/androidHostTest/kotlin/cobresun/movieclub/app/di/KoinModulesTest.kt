package cobresun.movieclub.app.di

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import okio.Path
import kotlin.test.Test

/** Every constructor dependency in the app graph has a definition (checked without creating anything). */
@OptIn(KoinExperimentalAPI::class)
class KoinModulesTest {
    @Test
    fun appGraphIsComplete() {
        module { includes(appModule, platformModule) }.verify(
            // Supplied at runtime by `androidContext()` and the ViewModel framework; the
            // offline store's directory is passed by hand in the platform module.
            extraTypes = listOf(Context::class, SavedStateHandle::class, Path::class),
        )
    }
}
