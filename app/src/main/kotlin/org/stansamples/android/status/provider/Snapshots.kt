package org.stansamples.android.status.provider

internal interface Snapshots {
    fun getSnapshot(): Map<String, String>
}
