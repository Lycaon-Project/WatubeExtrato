package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTimeoutPreemptively
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import java.time.Duration

/**
 * The sandbox that runs the functions taken from YouTube's player code.
 */
internal class JavaScriptTest {

    @Test
    fun runsAFunction() {
        assertEquals(
            "cba",
            JavaScript.run("function f(a) { return a.split('').reverse().join(''); }", "f", "abc"),
        )
    }

    @Test
    fun endlessLoopIsStopped() {
        assertStopped("function f(a) { while (true) {} }")
    }

    @Test
    fun endlessLoopCannotCatchTheStop() {
        assertStopped("function f(a) { for (;;) { try { while (true) {} } catch (e) {} } }")
    }

    @Test
    fun endlessLoopCannotSwallowTheStopInAFinally() {
        // a finally block runs for an exception and its continue would have dropped it
        assertStopped(
            "function f(a) { for (;;) { try { while (true) {} } finally { continue; } } }",
        )
    }

    @Test
    fun growingAllocationCannotSwallowTheStopInAFinally() {
        assertStopped(
            "function f(a) { var k = []; for (;;) { try { " +
                "while (true) { k.push(new Array(10000).join('x')); } " +
                "} finally { continue; } } }",
        )
    }

    @Test
    fun nativeCodeThatNeverChecksTheTimeIsGivenUp() {
        // a backtracking regular expression runs in Rhino's native code, between two
        // instructions: only the watchdog of the caller stops waiting for it
        assertStopped("function f(a) { return /^(a+)+$/.test('" + "a".repeat(40) + "!'); }")
    }

    @Test
    fun noJavaPackages() {
        assertThrows(RuntimeException::class.java) {
            JavaScript.run(
                "function f(a) { return java.lang.System.getProperty('user.home'); }",
                "f",
                "a",
            )
        }
        assertThrows(RuntimeException::class.java) {
            JavaScript.run("function f(a) { return Packages.java.io.File; }", "f", "a")
        }
    }

    @Test
    fun noJavaObjectBehindTheArguments() {
        assertThrows(RuntimeException::class.java) {
            JavaScript.run("function f(a) { return a.getClass().getName(); }", "f", "a")
        }
    }

    @Test
    fun deepRecursionFailsCleanly() {
        // a Rhino exception, not a StackOverflowError that would escape the callers
        assertThrows(RuntimeException::class.java) {
            JavaScript.run("function f(a) { return f(a); }", "f", "a")
        }
    }

    @Test
    fun oversizedCodeIsRefusedBeforeRunning() {
        val code = "function f(a) { return a; }//" + "x".repeat(JavaScript.MAX_SOURCE_LENGTH)
        assertThrows(IllegalArgumentException::class.java) { JavaScript.run(code, "f", "a") }
        assertThrows(IllegalArgumentException::class.java) { JavaScript.compileOrThrow(code) }
    }

    @Test
    fun oversizedResultIsRefused() {
        // 2^17 characters, far more than any signature
        assertThrows(IllegalStateException::class.java) {
            JavaScript.run(
                "function f(a) { var s = 'x'; for (var i = 0; i < 17; i++) { s += s; } return s; }",
                "f",
                "a",
            )
        }
    }

    @Test
    fun growingAllocationIsStoppedLongBeforeTheMemoryIsFull() {
        // 10 kB more at each turn: stopped by the memory limit, well before the time limit
        assertStopped(
            "function f(a) { var k = []; " +
                "while (true) { k.push(new Array(10000).join('x')); } }",
        )
    }

    @Test
    fun singleHugeAllocationFailsCleanly() {
        // one native call asking for gigabytes: an exception, not an OutOfMemoryError
        assertStopped("function f(a) { return new Array(1500000000).join('xy'); }")
    }

    /** The function never returns by itself: the sandbox stops it with an IllegalStateException. */
    private fun assertStopped(function: String) {
        assertTimeoutPreemptively(
            Duration.ofSeconds(30),
            Executable {
                assertThrows(IllegalStateException::class.java) {
                    JavaScript.run(function, "f", "a")
                }
            },
        )
    }
}
