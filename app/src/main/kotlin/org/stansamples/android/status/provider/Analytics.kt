package org.stansamples.android.status.provider

internal interface Analytics {
    fun report(key: String, payload: Map<String, String> = emptyMap())
}
