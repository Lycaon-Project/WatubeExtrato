package org.schabi.newpipe.extractor.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import org.junit.jupiter.api.Test;

import java.time.Duration;

/**
 * The sandbox that runs the functions taken from YouTube's player code.
 */
class JavaScriptTest {

    @Test
    void runsAFunction() {
        assertEquals("cba", JavaScript.run(
                "function f(a) { return a.split('').reverse().join(''); }", "f", "abc"));
    }

    @Test
    void endlessLoopIsStopped() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> assertThrows(
                IllegalStateException.class,
                () -> JavaScript.run("function f(a) { while (true) {} }", "f", "a")));
    }

    @Test
    void endlessLoopCannotCatchTheStop() {
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> assertThrows(
                IllegalStateException.class,
                () -> JavaScript.run(
                        "function f(a) { for (;;) { try { while (true) {} } catch (e) {} } }",
                        "f", "a")));
    }

    @Test
    void noJavaPackages() {
        assertThrows(RuntimeException.class, () -> JavaScript.run(
                "function f(a) { return java.lang.System.getProperty('user.home'); }", "f", "a"));
        assertThrows(RuntimeException.class, () -> JavaScript.run(
                "function f(a) { return Packages.java.io.File; }", "f", "a"));
    }

    @Test
    void noJavaObjectBehindTheArguments() {
        assertThrows(RuntimeException.class, () -> JavaScript.run(
                "function f(a) { return a.getClass().getName(); }", "f", "a"));
    }

    @Test
    void deepRecursionFailsCleanly() {
        // a Rhino exception, not a StackOverflowError that would escape the callers
        assertThrows(RuntimeException.class, () -> JavaScript.run(
                "function f(a) { return f(a); }", "f", "a"));
    }

    @Test
    void oversizedCodeIsRefusedBeforeRunning() {
        final String code = "function f(a) { return a; }//"
                + "x".repeat(JavaScript.MAX_SOURCE_LENGTH);
        assertThrows(IllegalArgumentException.class, () -> JavaScript.run(code, "f", "a"));
        assertThrows(IllegalArgumentException.class, () -> JavaScript.compileOrThrow(code));
    }

    @Test
    void oversizedResultIsRefused() {
        // 2^17 characters, far more than any signature
        assertThrows(IllegalStateException.class, () -> JavaScript.run(
                "function f(a) { var s = 'x'; for (var i = 0; i < 17; i++) { s += s; } "
                        + "return s; }", "f", "a"));
    }

    @Test
    void growingAllocationIsStoppedLongBeforeTheMemoryIsFull() {
        // 10 kB more at each turn: stopped by the memory limit, well before the time limit
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> assertThrows(
                IllegalStateException.class,
                () -> JavaScript.run("function f(a) { var k = []; "
                        + "while (true) { k.push(new Array(10000).join('x')); } }", "f", "a")));
    }

    @Test
    void singleHugeAllocationFailsCleanly() {
        // one native call asking for gigabytes: an exception, not an OutOfMemoryError
        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> assertThrows(
                IllegalStateException.class,
                () -> JavaScript.run(
                        "function f(a) { return new Array(1500000000).join('xy'); }",
                        "f", "a")));
    }
}
