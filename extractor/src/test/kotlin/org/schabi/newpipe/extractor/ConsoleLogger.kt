package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.utils.Logger

/**
 * Logger printing the extractor logs to the console, used by the tests only.
 */
class ConsoleLogger : Logger {
    override fun debug(tag: String, message: String?) {
        println("[DEBUG][$tag] $message")
    }

    override fun debug(tag: String, message: String?, throwable: Throwable) {
        debug(tag, message)
        throwable.printStackTrace(System.err)
    }

    override fun warn(tag: String, message: String?) {
        println("[WARN ][$tag] $message")
    }

    override fun warn(tag: String, message: String?, throwable: Throwable) {
        warn(tag, message)
        throwable.printStackTrace(System.err)
    }

    override fun error(tag: String, message: String?) {
        System.err.println("[ERROR][$tag] $message")
    }

    override fun error(tag: String, message: String?, t: Throwable) {
        System.err.println("[ERROR][$tag] $message")
        t.printStackTrace(System.err)
    }
}
