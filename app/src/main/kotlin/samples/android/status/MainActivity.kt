package samples.android.status

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.storage.StorageManager
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class MainActivity : ComponentActivity() {
    private val providers = App.providers
    private val logger = providers.loggers.create("[Main]")

    private fun updateStatus(context: Context, tv: TextView) {
        val am = context.getSystemService(ActivityManager::class.java)
        val ami = ActivityManager.MemoryInfo()
        am.getMemoryInfo(ami)
        val dmi = Debug.MemoryInfo()
        Debug.getMemoryInfo(dmi)
        val statFs = StatFs(Environment.getDataDirectory().path)
        val ssm = context.getSystemService(StorageStatsManager::class.java)
        val ss = ssm.queryStatsForPackage(
            StorageManager.UUID_DEFAULT,
            context.packageName,
            Process.myUserHandle(),
        )
        tv.text = """
            totalMem: ${ami.totalMem} (${ami.totalMem.toDouble().div(1024).div(1024).toLong()}mb)
            availMem: ${ami.availMem} (${ami.availMem.toDouble().div(1024).div(1024).toLong()}mb)
            threshold: ${ami.threshold} (${ami.threshold.toDouble().div(1024).div(1024).toLong()}mb)
            totalPss: ${dmi.totalPss * 1024L} (${dmi.totalPss.toDouble().div(1024).toLong()}mb)
            isLowMemory: ${ami.lowMemory}
            ---
            totalBytes: ${statFs.totalBytes} (${statFs.totalBytes.toDouble().div(1024).div(1024).toLong()}mb)
            freeBytes: ${statFs.freeBytes} (${statFs.freeBytes.toDouble().div(1024).div(1024).toLong()}mb)
            appBytes: ${ss.appBytes} (${ss.appBytes.toDouble().div(1024).div(1024).toLong()}mb)
            dataBytes: ${ss.dataBytes} (${ss.dataBytes.toDouble().div(1024).div(1024).toLong()}mb)
            cacheBytes: ${ss.cacheBytes} (${ss.cacheBytes.toDouble().div(1024).div(1024).toLong()}mb)
        """.trimIndent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context: Context = this
        LinearLayout(context).also { root ->
            root.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            root.orientation = LinearLayout.VERTICAL
            root.gravity = Gravity.CENTER_VERTICAL
            val statusText = TextView(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                root.addView(view)
            }
            Button(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                view.text = "update status"
                view.setOnClickListener { _ ->
                    updateStatus(context = context, statusText)
                }
                root.addView(view)
            }
            Button(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                view.text = "gc"
                view.setOnClickListener { _ ->
                    System.gc()
                }
                root.addView(view)
            }
            val deviceOwnerTitle = TextView(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                root.addView(view)
            }
            val deviceOwnerButton = Button(context).also { view ->
                view.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                view.text = "remove admin"
                view.setOnClickListener { _ ->
                    providers.admins.update(isDeviceOwner = false)
                }
                root.addView(view)
            }
            lifecycleScope.launch {
                withContext(providers.contexts.default) {
                    providers.admins.owners.collect { isDeviceOwner ->
                        logger.debug("device:owner: $isDeviceOwner")
                        withContext(providers.contexts.main) {
                            deviceOwnerTitle.text = "device:owner: $isDeviceOwner"
                            deviceOwnerButton.isEnabled = isDeviceOwner
                        }
                    }
                }
            }
            setContentView(root)
        }
    }
}
