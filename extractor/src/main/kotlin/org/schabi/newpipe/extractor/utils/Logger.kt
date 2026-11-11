package org.schabi.newpipe.extractor.utils

/** Receives the logs of the extractor; see [ExtractorLogger.setLogger]. */
public interface Logger {
    public fun debug(tag: String, message: String?)
    public fun debug(tag: String, message: String?, throwable: Throwable)
    public fun warn(tag: String, message: String?)
    public fun warn(tag: String, message: String?, throwable: Throwable)
    public fun error(tag: String, message: String?)
    public fun error(tag: String, message: String?, t: Throwable)
}
