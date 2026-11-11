package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * See [org.schabi.newpipe.extractor.LoggerExtension] for how global logging is set up for tests
 */
internal class ExtractorLoggerTest {

    private lateinit var logger: CapturingLogger

    @BeforeEach
    fun setup() {
        logger = CapturingLogger()
        ExtractorLogger.setLogger(logger)
    }

    @Test
    fun replacesSinglePlaceholder() {
        ExtractorLogger.d("T", "Hello {Name}", "Alice")
        assertEquals("Hello Alice", logger.lastDebug)
    }

    @Test
    fun replacesMultiplePlaceholdersSequentially() {
        ExtractorLogger.d("T", "A={A} B={B} C={C}", 1, 2, 3)
        assertEquals("A=1 B=2 C=3", logger.lastDebug)
    }

    @Test
    fun leavesExtraPlaceholdersWhenNotEnoughArgs() {
        ExtractorLogger.d("T", "First={F} Second={S} Third={T}", "X", "Y")
        assertEquals("First=X Second=Y Third={T}", logger.lastDebug)
    }

    @Test
    fun ignoresExtraArgs() {
        ExtractorLogger.d("T", "Only {One}", "X", "Y", "Z")
        assertEquals("Only X", logger.lastDebug)
    }

    @Test
    fun noArgsReturnsTemplateUnchanged() {
        ExtractorLogger.d("T", "No placeholders {} here")
        assertEquals("No placeholders {} here", logger.lastDebug)
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "Value {Unclosed,Value {Unclosed",
            "{({test_one)}}},}",
            "{{test},{test}",
            "{{test_three}}},{test_three}}",
        ],
    )
    fun unmatchedBraceLeavesRemainder(message: String, expected: String) {
        ExtractorLogger.d("T", message, "")
        assertEquals(expected, logger.lastDebug)
    }

    @ParameterizedTest
    @CsvSource(
        "{{,{",
        "}},}",
        "{{}},{}",
        "}}{{,}{",
        "hello},hello}",
        "hello{,hello{",
    )
    fun doubleBraceEscaping(message: String, expected: String) {
        ExtractorLogger.d("T", message, "unconsumed")
        assertEquals(expected, logger.lastDebug)
    }

    @Test
    fun escapedBracesDoNotConsumeArg() {
        ExtractorLogger.d("T", "{{Name}}", "Alice")
        assertEquals("{Name}", logger.lastDebug)
    }

    @Test
    fun escapedBraceThenPlaceholder() {
        ExtractorLogger.d("T", "{{literal}} {Value}", "Alice")
        assertEquals("{literal} Alice", logger.lastDebug)
    }

    @ParameterizedTest
    @CsvSource(
        "Value {Unclosed,Value {Unclosed",
        "{({test_one)}}},}",
    )
    fun unclosedOrStrayBracesLeaveRemainderUnchanged(message: String, expected: String) {
        ExtractorLogger.d("T", message, "")
        assertEquals(expected, logger.lastDebug)
    }

    @ParameterizedTest
    @CsvSource(
        "{{test},{test}",
        "{{test_three}}},{test_three}}",
    )
    fun doubleBraceEscapesLiteralBraces(message: String, expected: String) {
        ExtractorLogger.d("T", message, "")
        assertEquals(expected, logger.lastDebug)
    }

    @Test
    fun tripleOpenBraceIsEscapedLiteralThenPlaceholder() {
        ExtractorLogger.d("T", "{{{Name}}}", "Alice")
        assertEquals("{Alice}", logger.lastDebug)
    }

    @Test
    fun mixedEscapedAndPlaceholders() {
        ExtractorLogger.d("T", "{A} {{B}} {C}", "1", "2")
        assertEquals("1 {B} 2", logger.lastDebug)
    }

    @Test
    fun nullArgFormatsAsNullString() {
        // a single null argument, not a null Throwable
        ExtractorLogger.d("T", "value is {V}", *arrayOf<Any?>(null))
        assertEquals("value is null", logger.lastDebug)
    }

    @Test
    fun emptyTemplateWithArgsReturnsEmpty() {
        ExtractorLogger.d("T", "", "X")
        assertEquals("", logger.lastDebug)
    }

    @Test
    fun nullTemplatePrintsNull() {
        val template: String? = null
        ExtractorLogger.d("T", template, "X")
        assertNull(logger.lastDebug)
    }

    @Test
    fun escapedBracesWithThrowable() {
        val ex = RuntimeException("boom")
        ExtractorLogger.d("T", ex, "{{literal}} {Value}", "Alice")
        assertEquals("{literal} Alice", logger.lastDebug)
        assertSame(ex, logger.lastDebugThrowable)
    }

    @Test
    fun debugFormatWithThrowable() {
        val ex = RuntimeException("boom")
        ExtractorLogger.d("T", ex, "Failure {Code} at {Step}", 500, "init")
        assertEquals("Failure 500 at init", logger.lastDebug)
        assertSame(ex, logger.lastDebugThrowable)
    }

    @Test
    fun warnFormatWithThrowable() {
        val ex = IllegalStateException("warned")
        ExtractorLogger.w("T", ex, "Warn {What}", "disk")
        assertEquals("Warn disk", logger.lastWarn)
        assertSame(ex, logger.lastWarnThrowable)
    }

    @Test
    fun errorFormatWithThrowable() {
        val ex = Exception("fatal")
        ExtractorLogger.e("T", ex, "Error {Type} code={Code}", "IO", 42)
        assertEquals("Error IO code=42", logger.lastError)
        assertSame(ex, logger.lastErrorThrowable)
    }

    @Test
    fun debugFormatWithThrowableNotEnoughArgsLeavesPlaceholder() {
        val ex = RuntimeException("x")
        ExtractorLogger.d("T", ex, "Only one {A} and leftover {B}", "arg1")
        assertEquals("Only one arg1 and leftover {B}", logger.lastDebug)
        assertSame(ex, logger.lastDebugThrowable)
    }

    @Test
    fun errorFormatWithThrowableExtraArgsIgnored() {
        val ex = Exception("x")
        ExtractorLogger.e("T", ex, "Val {V}", 10, 20, 30)
        assertEquals("Val 10", logger.lastError)
        assertSame(ex, logger.lastErrorThrowable)
    }

    private class CapturingLogger : Logger {
        var lastDebug: String? = null
        var lastDebugThrowable: Throwable? = null
        var lastWarn: String? = null
        var lastWarnThrowable: Throwable? = null
        var lastError: String? = null
        var lastErrorThrowable: Throwable? = null

        override fun debug(tag: String, message: String?) {
            lastDebug = message
            lastDebugThrowable = null
        }

        override fun debug(tag: String, message: String?, throwable: Throwable) {
            lastDebug = message
            lastDebugThrowable = throwable
        }

        override fun warn(tag: String, message: String?) {
            lastWarn = message
            lastWarnThrowable = null
        }

        override fun warn(tag: String, message: String?, throwable: Throwable) {
            lastWarn = message
            lastWarnThrowable = throwable
        }

        override fun error(tag: String, message: String?) {
            lastError = message
            lastErrorThrowable = null
        }

        override fun error(tag: String, message: String?, t: Throwable) {
            lastError = message
            lastErrorThrowable = t
        }
    }
}
