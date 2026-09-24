package org.stansamples.android.status.provider

import android.os.Environment
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.stansamples.android.status.BuildConfig
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

internal class FinalAnalytics(
    private val coroutineScope: CoroutineScope,
    private val contexts: Contexts,
    private val snapshots: Snapshots,
    loggers: Loggers,
) : Analytics {
    private val logger = loggers.create("[Analytics]")
    private val launched = System.currentTimeMillis().milliseconds
    private val dirFormat = dateFormat(pattern = "yyyyMMdd", locale = Locale.US, timeZone = TimeZone.getTimeZone("utc"))
    private val fileFormat = dateFormat(pattern = "yyyyMMddHH", locale = Locale.US, timeZone = TimeZone.getTimeZone("utc"))

    private val locks = ReentrantReadWriteLock()
    private val indices = AtomicLong(launched.inWholeMilliseconds)

    init {
        var timeStart = SystemClock.elapsedRealtime().milliseconds
        val timeMax = 16.seconds
        coroutineScope.launch {
            withContext(contexts.default) {
                while (true) {
                    val timeNow = SystemClock.elapsedRealtime().milliseconds
                    if (timeNow.minus(timeStart) < timeMax) {
                        delay(1.seconds)
                        continue
                    }
                    report(key = "periodic status", payload = snapshots.getSnapshot())
                    timeStart = SystemClock.elapsedRealtime().milliseconds
                }
            }
        }
    }

    private fun toJSONObject(date: Date, key: String, payload: Map<String, String>): JSONObject {
        val obj = JSONObject()
        obj.put("id", indices.incrementAndGet())
        obj.put("timestamp", date.time)
        obj.put("key", key)
        if (payload.isNotEmpty()) {
            val entries = JSONObject()
            payload.forEach { (key, value) ->
                entries.put(key, value)
            }
            obj.put("payload", entries)
        }
        return obj
    }

    override fun report(key: String, payload: Map<String, String>) {
        coroutineScope.launch {
            withContext(contexts.io) {
                locks.writeLock().withLock {
                    logger.debug("event: $key")
                    val date = Date()
                    val docs = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                        ?: error("No docs!")
                    val fileName = "Analytics_${fileFormat.format(date)}_${launched.inWholeMilliseconds}.jsonl"
                    for (file in docs.resolve(BuildConfig.APPLICATION_ID).resolve("Analytics").listFiles().orEmpty()) {
                        if (!file.exists()) continue
                        if (!file.isDirectory) continue
                        val then = try {
                            fileFormat.parse(file.name)
                        } catch (_: Throwable) {
                            continue
                        }.time.milliseconds
                        if (date.time.milliseconds.minus(then) > 14.days) {
                            file.deleteRecursively()
                        }
                    }
                    val dir = docs
                        .resolve(BuildConfig.APPLICATION_ID)
                        .resolve("Analytics")
                        .resolve(dirFormat.format(date))
                    dir.mkdirs()
                    val file = dir.resolve(fileName)
                    logger.debug("file: ${file.absolutePath}")
                    val text = StringBuilder()
                    if (file.exists()) text.append("\n")
                    text.append(toJSONObject(date = date, key = key, payload = payload).toString())
                    file.appendText(text.toString())
                }
            }
        }
    }

    companion object {
        private fun dateFormat(pattern: String, locale: Locale = Locale.getDefault(), timeZone: TimeZone = TimeZone.getDefault()): DateFormat {
            val dateFormat = SimpleDateFormat(pattern, locale)
            dateFormat.timeZone = timeZone
            return dateFormat
        }
    }
}
