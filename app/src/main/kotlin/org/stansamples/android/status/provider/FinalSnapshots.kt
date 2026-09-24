package org.stansamples.android.status.provider

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.os.SystemClock
import android.os.storage.StorageManager
import org.stansamples.android.status.BuildConfig

internal class FinalSnapshots(private val context: Context) : Snapshots {
    override fun getSnapshot(): Map<String, String> {
        val snapshot = mutableMapOf<String, String>()
        //
        val am = context.getSystemService(ActivityManager::class.java)
        val ami = ActivityManager.MemoryInfo()
        am.getMemoryInfo(ami)
        val dmi = Debug.MemoryInfo()
        Debug.getMemoryInfo(dmi)
        snapshot["os:ram"] = "${ami.totalMem}/${ami.availMem}/${dmi.totalPss * 1024L}"
        //
        val statFs = StatFs(Environment.getDataDirectory().path)
        val ssm = context.getSystemService(StorageStatsManager::class.java)
        val ss = ssm.queryStatsForPackage(
            StorageManager.UUID_DEFAULT,
            context.packageName,
            Process.myUserHandle(),
        )
        snapshot["os:fs"] = "${statFs.totalBytes}/${statFs.freeBytes}/${ss.appBytes + ss.dataBytes}(ss.cacheBytes)"
        //
        snapshot["os:elapsed"] = "${SystemClock.elapsedRealtime()}"
        //
        val cm = context.getSystemService(ConnectivityManager::class.java)
        for (network in cm.allNetworks) {
            val nc = cm.getNetworkCapabilities(network) ?: continue
            if (!nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) continue
            snapshot["os:ethernet"] = listOf(
                "default:${cm.activeNetwork == network}",
                "internet:${nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)}",
                "validated:${nc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)}",
            ).joinToString(separator = "/")
            break
        }
        snapshot["os:versions"] = listOf(
            "${Build.VERSION.RELEASE}",
            "sdk:${Build.VERSION.SDK_INT}",
            "fingerprint:${Build.FINGERPRINT}",
        ).joinToString(separator = "/")
        //
        val ei = am.getHistoricalProcessExitReasons(context.packageName, 0, 8).maxByOrNull { it.timestamp }
        if (ei != null) {
            snapshot["app:exits"] = "${ei.timestamp}/${ei.reason}/${ei.status}"
        }
        //
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        snapshot["app:times"] = "${pi.firstInstallTime}/${pi.lastUpdateTime}"
        //
        snapshot["app:versions"] = listOf(
            BuildConfig.APPLICATION_ID,
            "build_type:${BuildConfig.BUILD_TYPE}",
            "version:${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}",
        ).joinToString(separator = "/")
        //
        return snapshot
    }
}
