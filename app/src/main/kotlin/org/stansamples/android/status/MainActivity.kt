package org.stansamples.android.status

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds

internal class MainActivity : ComponentActivity() {
    private val providers = App.providers
    private val logger = providers.loggers.create("[Main]")

    private fun onNetwork(cm: ConnectivityManager, network: Network?, dst: MutableMap<String, String>) {
        if (network == null) return
        val nc = cm.getNetworkCapabilities(network) ?: return
        val lp = cm.getLinkProperties(network) ?: return
        val transport = when {
            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "eth"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_USB) -> "usb"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "bt"
            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            else -> return
        }
        val key = lp.interfaceName
        if (key.isNullOrBlank()) return
        if (dst.containsKey("$key:transport")) return
        if (cm.activeNetwork == network) {
            dst["$key:default"] = "true"
        }
        dst["$key:transport"] = transport
        dst["$key:internet"] = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).toString()
        dst["$key:validated"] = nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED).toString()
        dst["$key:linkAddresses"] = lp.linkAddresses.toList().toString()
    }

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
        val elapsed = SystemClock.elapsedRealtime().milliseconds
        val now = System.currentTimeMillis().milliseconds
        val pi = packageManager.getPackageInfo(context.packageName, 0)
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val networks = mutableMapOf<String, String>()
        cm.allNetworks.forEach { network ->
            onNetwork(cm = cm, network = network, dst = networks)
        }
        val ei = mutableMapOf<String, String>()
        am.getHistoricalProcessExitReasons(context.packageName, 0, 8).maxByOrNull {
            it.timestamp
        }?.also {
            ei["ei:timestamp"] = it.timestamp.toString()
            ei["ei:reason"] = it.reason.toString()
            ei["ei:status"] = it.status.toString()
            ei["ei:pss"] = it.pss.toString()
            ei["ei:rss"] = it.rss.toString()
        }
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
            ---
            elapsed: ${elapsed.inWholeMilliseconds}ms
            booted: ${now.minus(elapsed).inWholeMilliseconds} (${Date(now.minus(elapsed).inWholeMilliseconds)})
            installed: ${pi.firstInstallTime} (${Date(pi.firstInstallTime)})
            updated: ${pi.lastUpdateTime} (${Date(pi.lastUpdateTime)})
            ---
            build.version.release: ${Build.VERSION.RELEASE}
            build.version.sdk: ${Build.VERSION.SDK_INT}
            build.display: ${Build.DISPLAY}
            build.id: ${Build.ID}
            build.fingerprint: ${Build.FINGERPRINT}
            ---
            APPLICATION_ID: ${BuildConfig.APPLICATION_ID}
            BUILD_TYPE: ${BuildConfig.BUILD_TYPE}
            VERSION_NAME: ${BuildConfig.VERSION_NAME}
            VERSION_CODE: ${BuildConfig.VERSION_CODE}
            ---
            networks: $networks
            ---
            ei: $ei
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
            root.updatePadding(top = 300, bottom = 128)
            ScrollView(context).also { sv ->
                sv.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                    1f,
                )
                val tv = TextView(context).also { view ->
                    view.layoutParams = LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    sv.addView(view)
                }
                root.addView(sv)
                Button(context).also { view ->
                    view.layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    view.text = "update status"
                    view.setOnClickListener { _ ->
                        updateStatus(context = context, tv = tv)
                    }
                    root.addView(view)
                }
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
