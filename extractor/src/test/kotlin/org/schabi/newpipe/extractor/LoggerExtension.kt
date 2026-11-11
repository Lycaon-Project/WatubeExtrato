package org.schabi.newpipe.extractor

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.schabi.newpipe.extractor.utils.ExtractorLogger

/**
 * JUnit extension class for globally setting up the logger for all extractor tests<br>
 * See [here](https://www.baeldung.com/junit-5-extensions#1-automatic-extension-registration)
 * on how this works.
 *
 * To disable this, set `junit.jupiter.extensions.autodetection.enabled = false`
 * in junit-platform.properties
 */
class LoggerExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        if (set) return
        set = true
        ExtractorLogger.setLogger(ConsoleLogger())
    }

    private companion object {
        var set = false
    }
}
