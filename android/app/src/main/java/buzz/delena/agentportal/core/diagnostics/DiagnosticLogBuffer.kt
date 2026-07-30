package buzz.delena.agentportal.core.diagnostics

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * In-process ring buffer for troubleshooting dumps uploaded to the portal.
 * Caps at [MAX_LINES] / roughly [MAX_CHARS] so uploads stay under the server limit.
 */
object DiagnosticLogBuffer {
    private const val MAX_LINES = 2000
    private const val MAX_CHARS = 256 * 1024

    private val lines = ConcurrentLinkedDeque<String>()
    private var charCount = 0
    private val lock = Any()
    private val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun append(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val ts = synchronized(timeFmt) { timeFmt.format(Date()) }
        val base = "$ts $level/$tag: $message"
        val entry = if (throwable == null) {
            base
        } else {
            base + "\n" + Log.getStackTraceString(throwable).trimEnd()
        }
        synchronized(lock) {
            lines.addLast(entry)
            charCount += entry.length + 1
            while (lines.size > MAX_LINES || charCount > MAX_CHARS) {
                val removed = lines.pollFirst() ?: break
                charCount -= removed.length + 1
            }
            if (charCount < 0) charCount = 0
        }
    }

    fun snapshot(): String = synchronized(lock) {
        lines.joinToString("\n")
    }

    fun clear() = synchronized(lock) {
        lines.clear()
        charCount = 0
    }
}

/** Thin Log wrapper that also feeds [DiagnosticLogBuffer]. */
object AppLog {
    fun d(tag: String, msg: String) {
        DiagnosticLogBuffer.append("D", tag, msg)
        Log.d(tag, msg)
    }

    fun i(tag: String, msg: String) {
        DiagnosticLogBuffer.append("I", tag, msg)
        Log.i(tag, msg)
    }

    fun w(tag: String, msg: String, t: Throwable? = null) {
        DiagnosticLogBuffer.append("W", tag, msg, t)
        if (t == null) Log.w(tag, msg) else Log.w(tag, msg, t)
    }

    fun e(tag: String, msg: String, t: Throwable? = null) {
        DiagnosticLogBuffer.append("E", tag, msg, t)
        if (t == null) Log.e(tag, msg) else Log.e(tag, msg, t)
    }
}
