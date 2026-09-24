package org.stansamples.android.status

import android.app.Application
import android.content.Context
import android.os.Environment
import android.util.AtomicFile
import androidx.core.util.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.stansamples.android.status.provider.Admins
import org.stansamples.android.status.provider.Analytics
import org.stansamples.android.status.provider.Contexts
import org.stansamples.android.status.provider.FinalAdmins
import org.stansamples.android.status.provider.FinalAnalytics
import org.stansamples.android.status.provider.FinalLoggers
import org.stansamples.android.status.provider.FinalSnapshots
import org.stansamples.android.status.provider.Loggers
import org.stansamples.android.status.provider.Providers
import org.stansamples.android.status.provider.Snapshots

internal class App : Application() {
    override fun onCreate() {
        super.onCreate()
        val context: Context = this
        val loggers: Loggers = FinalLoggers()
        val contexts = Contexts(
            main = Dispatchers.Main,
            default = Dispatchers.Default,
            io = Dispatchers.IO,
        )
        val admins: Admins = FinalAdmins(
            context = context,
            loggers = loggers,
        )
        val job = SupervisorJob()
        val coroutineScope = CoroutineScope(contexts.main + job)
        val snapshots: Snapshots = FinalSnapshots(context = context)
        val analytics: Analytics = FinalAnalytics(
            coroutineScope = coroutineScope,
            contexts = contexts,
            loggers = loggers,
            snapshots = snapshots,
        )
        val handler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error: Throwable ->
            val text = """
                timestamp: ${System.currentTimeMillis()}
                error: ${error::class.java.name}
                cause: ${error.cause?.let {it::class.java.name}}
            """.trimIndent()
            try {
                Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                    .resolve(BuildConfig.APPLICATION_ID)
                    .resolve("uncaught_exception.txt")
                    .let(::AtomicFile)
                    .writeText(text)
            } catch (_: Throwable) {
                // noop
            }
            handler?.uncaughtException(thread, error)
        }
        _providers = Providers(
            loggers = loggers,
            contexts = contexts,
            admins = admins,
            analytics = analytics,
        )
    }

    companion object {
        private var _providers: Providers? = null
        val providers: Providers get() = checkNotNull(_providers) { "No providers!" }
    }
}
