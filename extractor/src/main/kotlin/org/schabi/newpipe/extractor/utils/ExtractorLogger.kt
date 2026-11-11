package org.schabi.newpipe.extractor.utils

import kotlin.math.min

/**
 * Logging class for outputting logs from the extractor to the desired output
 *
 * Intended to be used in the same manner as Android's `Log`:
 * `ExtractorLogger.d("Hello my name Jeff")`
 *
 * Also supports formatted arguments:
 * `ExtractorLogger.d("Hello my name is {Name} {}", name, surname)`
 *
 * Nothing is formatted while no logger is set, so that logging costs nothing in release.
 */
public object ExtractorLogger {

    private const val FORMAT_EXTRA_CAPACITY_PER_ARG = 16
    private const val FORMAT_MAX_EXTRA_CAPACITY = 32

    @Volatile
    private var logger: Logger = EmptyLogger

    /**
     * Set the Logger that you want the extractor logs to be logged to
     *
     * Provide an implementation of the `Logger` interface for each method and whenever the
     * extractor code calls `ExtractorLogger.d/w/e`
     * it will be routed through to `customLogger`
     *
     * For NewPipe, this should be set at the start of the application ideally in
     * `MainActivity.onCreate`, but absolutely before any extractor code can run
     */
    @JvmStatic
    public fun setLogger(customLogger: Logger?) {
        logger = customLogger ?: EmptyLogger
    }

    public enum class Level { DEBUG, WARN, ERROR }

    private fun log(level: Level, tag: String, message: String?, t: Throwable?) {
        val current = logger
        if (current === EmptyLogger) return
        when (level) {
            Level.DEBUG -> if (t == null) current.debug(tag, message) else current.debug(tag, message, t)
            Level.WARN -> if (t == null) current.warn(tag, message) else current.warn(tag, message, t)
            Level.ERROR -> if (t == null) current.error(tag, message) else current.error(tag, message, t)
        }
    }

    /**
     * Internal method for logging with formatting
     */
    private fun logFormat(
        level: Level,
        tag: String,
        t: Throwable?,
        template: String?,
        args: Array<out Any?>,
    ) {
        if (logger === EmptyLogger) return
        log(level, tag, format(template, args), t)
    }

    // DEBUG
    @JvmStatic
    public fun d(tag: String, msg: String?) {
        log(Level.DEBUG, tag, msg, null)
    }

    @JvmStatic
    public fun d(tag: String, msg: String?, t: Throwable?) {
        log(Level.DEBUG, tag, msg, t)
    }

    @JvmStatic
    public fun d(tag: String, template: String?, vararg args: Any?) {
        logFormat(Level.DEBUG, tag, null, template, args)
    }

    @JvmStatic
    public fun d(tag: String, t: Throwable?, template: String?, vararg args: Any?) {
        logFormat(Level.DEBUG, tag, t, template, args)
    }

    // WARN
    @JvmStatic
    public fun w(tag: String, msg: String?) {
        log(Level.WARN, tag, msg, null)
    }

    @JvmStatic
    public fun w(tag: String, msg: String?, t: Throwable?) {
        log(Level.WARN, tag, msg, t)
    }

    @JvmStatic
    public fun w(tag: String, template: String?, vararg args: Any?) {
        logFormat(Level.WARN, tag, null, template, args)
    }

    @JvmStatic
    public fun w(tag: String, t: Throwable?, template: String?, vararg args: Any?) {
        logFormat(Level.WARN, tag, t, template, args)
    }

    // ERROR
    @JvmStatic
    public fun e(tag: String, msg: String?) {
        log(Level.ERROR, tag, msg, null)
    }

    @JvmStatic
    public fun e(tag: String, msg: String?, t: Throwable?) {
        log(Level.ERROR, tag, msg, t)
    }

    @JvmStatic
    public fun e(tag: String, template: String?, vararg args: Any?) {
        logFormat(Level.ERROR, tag, null, template, args)
    }

    @JvmStatic
    public fun e(tag: String, t: Throwable?, template: String?, vararg args: Any?) {
        logFormat(Level.ERROR, tag, t, template, args)
    }

    /**
     * Simple string format method for easier logging in the form of
     * `ExtractorLogger.d("Hello my name {Name} {}", name, surname)`
     *
     * Braces can be escaped by double braces:
     * `{{ -> {` and
     * `}} -> }`
     * @param template The template string to format
     * @param args Arguments to replace identifiers with in `template`
     * @return Formatted string with arguments replaced
     */
    private fun format(template: String?, args: Array<out Any?>): String? {
        if (template == null || args.isEmpty()) {
            return template
        }
        val n = template.length
        val result = StringBuilder(
            n + min(FORMAT_MAX_EXTRA_CAPACITY, FORMAT_EXTRA_CAPACITY_PER_ARG * args.size),
        )
        var cursorIndex = 0
        var argIndex = 0
        while (cursorIndex < n) {
            val ch = template[cursorIndex]
            val next = if (cursorIndex + 1 < n) template[cursorIndex + 1] else null

            // {{ -> {
            // If current char is { and next char is {, append { and skip 2 chars
            // }} -> }
            if ((ch == '{' || ch == '}') && next == ch) {
                result.append(ch)
                cursorIndex += 2
                continue
            }

            // {name}, {} placeholders
            if (ch == '{') {
                // Find first } after the current {
                val closeBraceIndex = template.indexOf('}', cursorIndex + 1)
                if (closeBraceIndex < 0) {
                    // If none found then there's no more arguments to replace
                    // Append rest of the string
                    result.append(template, cursorIndex, n)
                    break
                }

                // Found a }
                if (argIndex < args.size) {
                    // Append the argument
                    result.append(args[argIndex++])
                } else {
                    // No args left to append; append text as is
                    result.append(template, cursorIndex, closeBraceIndex + 1)
                }
                cursorIndex = closeBraceIndex + 1
                continue
            }

            result.append(ch)
            cursorIndex++
        }
        return result.toString()
    }

    private object EmptyLogger : Logger {
        override fun debug(tag: String, message: String?): Unit = Unit
        override fun debug(tag: String, message: String?, throwable: Throwable): Unit = Unit
        override fun warn(tag: String, message: String?): Unit = Unit
        override fun warn(tag: String, message: String?, throwable: Throwable): Unit = Unit
        override fun error(tag: String, message: String?): Unit = Unit
        override fun error(tag: String, message: String?, t: Throwable): Unit = Unit
    }
}
