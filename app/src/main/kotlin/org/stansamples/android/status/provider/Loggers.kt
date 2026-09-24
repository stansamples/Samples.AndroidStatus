package org.stansamples.android.status.provider

internal interface Loggers {
    fun create(tag: String): Logger
}

internal interface Logger {
    fun debug(message: String)
    fun warning(message: String)
}
