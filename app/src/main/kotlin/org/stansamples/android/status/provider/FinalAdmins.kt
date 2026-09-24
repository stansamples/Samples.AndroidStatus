package org.stansamples.android.status.provider

import android.app.admin.DevicePolicyManager
import android.content.Context
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

internal class FinalAdmins(
    private val context: Context,
    loggers: Loggers,
) : Admins {
    private val logger = loggers.create("[Admins]")
    override val owners = object : StateFlow<Boolean> {
        override val value: Boolean
            get() {
                val dm = context.getSystemService(DevicePolicyManager::class.java)
                val isDeviceOwner = dm.isDeviceOwnerApp(context.packageName)
                return isDeviceOwner
            }
        override val replayCache: List<Boolean> = emptyList()

        override suspend fun collect(collector: FlowCollector<Boolean>): Nothing {
            val dm = context.getSystemService(DevicePolicyManager::class.java)
            val values = AtomicBoolean(dm.isDeviceOwnerApp(context.packageName))
            collector.emit(values.get())
            while (true) {
                val isDeviceOwner = dm.isDeviceOwnerApp(context.packageName)
                if (values.compareAndSet(!isDeviceOwner, isDeviceOwner)) {
                    collector.emit(isDeviceOwner)
                }
                delay(500.milliseconds)
            }
        }
    }

    override fun update(isDeviceOwner: Boolean) {
        if (owners.value == isDeviceOwner) {
            logger.debug("Param \"isDeviceOwner\" is already $isDeviceOwner.")
            return
        }
        if (isDeviceOwner) error("Set an app the device owner is not supported!")
        val dm = context.getSystemService(DevicePolicyManager::class.java)
        dm.clearDeviceOwnerApp(context.packageName)
    }
}
