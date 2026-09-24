package samples.android.status

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import samples.android.status.provider.Admins
import samples.android.status.provider.Contexts
import samples.android.status.provider.FinalAdmins
import samples.android.status.provider.FinalLoggers
import samples.android.status.provider.Loggers
import samples.android.status.provider.Providers

internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val context: Context = this
        val loggers: Loggers = FinalLoggers()
        val contexts = Contexts(
            main = Dispatchers.Main,
            default = Dispatchers.Default,
        )
        val admins: Admins = FinalAdmins(
            context = context,
            loggers = loggers,
        )
        _providers = Providers(
            loggers = loggers,
            contexts = contexts,
            admins = admins,
        )
    }

    companion object {
        private var _providers: Providers? = null
        val providers: Providers get() = checkNotNull(_providers) { "No providers!" }
    }
}
