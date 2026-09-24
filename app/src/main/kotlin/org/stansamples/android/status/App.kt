package org.stansamples.android.status

import android.app.Application
import android.content.Context
import kotlinx.coroutines.Dispatchers
import org.stansamples.android.status.provider.Admins
import org.stansamples.android.status.provider.Contexts
import org.stansamples.android.status.provider.FinalAdmins
import org.stansamples.android.status.provider.FinalLoggers
import org.stansamples.android.status.provider.Loggers
import org.stansamples.android.status.provider.Providers

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
