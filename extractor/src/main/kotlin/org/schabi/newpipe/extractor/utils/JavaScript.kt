package org.schabi.newpipe.extractor.utils

import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Function
import org.mozilla.javascript.Script
import org.mozilla.javascript.Scriptable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.Callable as Task
import kotlin.math.min

/**
 * Runs JavaScript code taken from a remote page (YouTube's player) in a locked-down Rhino
 * context: interpreted mode, standard objects only, no access to any Java class, and a bounded
 * run time and call depth, so that an unexpected or hostile script can neither reach the
 * application nor hang the thread running it.
 *
 * Rhino checks the run time between interpreted instructions only: a native function (a
 * backtracking regular expression, a huge join) runs without any check. So the code runs on a
 * thread of its own, and the caller gives it up when the time is over, whatever it is doing.
 *
 * An application can hand the runs to its own engine with
 * [setExternalJavaScriptInterpreter]; it then owns the isolation of that engine.
 */
public object JavaScript {
    @Volatile
    private var externalJavaScriptInterpreter: JavaScriptInterpreter? = null

    private val MAX_RUN_TIME_NANOS = TimeUnit.SECONDS.toNanos(5)

    // how many interpreted instructions run between two run time checks
    private const val INSTRUCTION_OBSERVER_THRESHOLD = 10_000
    private const val MAX_INTERPRETER_STACK_DEPTH = 1_000

    /** The whole player code is a few megabytes; the functions taken from it far less. */
    public const val MAX_SOURCE_LENGTH: Int = 8 * 1024 * 1024

    /** A signature or a throttling parameter: a few hundred characters. */
    private const val MAX_RESULT_LENGTH = 64 * 1024

    /**
     * How much more of the heap a run may hold: the time limit alone would let a script fill the
     * memory of a phone before it stops it.
     */
    private const val MAX_RUN_MEMORY_BYTES = 64L * 1024 * 1024

    /** A quarter of the heap at most, for the small heaps of some phones. */
    private const val HEAP_SHARE_DIVISOR = 4

    /**
     * The heap is measured for the whole app: other threads and garbage not yet collected count
     * too. A run is stopped for its memory only when the heap is really getting full, past half
     * of it, so that a busy app never makes a valid signature fail.
     */
    private const val HEAP_PRESSURE_DIVISOR = 2

    /** How long a caller waits past the run time limit before giving the run up. */
    private val WATCHDOG_GRACE_NANOS = TimeUnit.SECONDS.toNanos(1)

    /** Runs at the same time; a run given up keeps its thread until its native call ends. */
    private const val MAX_RUNNERS = 4
    private const val MAX_WAITING_RUNS = 64
    private const val RUNNER_KEEP_ALIVE_SECONDS = 30L

    /** Created on the first run: none at all when the app uses its own interpreter. */
    private val RUNNERS: ThreadPoolExecutor by lazy {
        ThreadPoolExecutor(
            MAX_RUNNERS,
            MAX_RUNNERS,
            RUNNER_KEEP_ALIVE_SECONDS,
            TimeUnit.SECONDS,
            ArrayBlockingQueue(MAX_WAITING_RUNS),
        ) { task -> Thread(task, "JavaScript sandbox").apply { isDaemon = true } }
            .apply { allowCoreThreadTimeOut(true) }
    }

    private val SANDBOX: ContextFactory = SandboxContextFactory()

    /**
     * The scripts compiled last, by source: the signature and the throttling functions of the
     * current player run for every stream, so they are parsed once. An interpreted script holds
     * no state of its own: it runs in a new scope each time, on any thread.
     */
    private const val MAX_COMPILED_SCRIPTS = 4
    private val COMPILED_SCRIPTS = object : LinkedHashMap<String, Script>(MAX_COMPILED_SCRIPTS, 1f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Script>): Boolean =
            size > MAX_COMPILED_SCRIPTS
    }

    @JvmStatic
    public fun compileOrThrow(function: String) {
        checkSourceLength(function)
        try {
            SANDBOX.enterContext().use { context ->
                // If it doesn't compile it throws an exception here
                context.compileString(function, null, 1, null)
            }
        } catch (e: OutOfMemoryError) {
            // a huge or deeply nested source: refused like any code that does not compile
            throw IllegalStateException("JavaScript code too complex", e)
        } catch (e: StackOverflowError) {
            throw IllegalStateException("JavaScript code too complex", e)
        }
    }

    @JvmStatic
    public fun run(function: String, functionName: String, vararg parameters: String): String {
        val external = externalJavaScriptInterpreter
        if (external != null) {
            return external.run(function, functionName, *parameters)
        }

        checkSourceLength(function)
        val run = try {
            RUNNERS.submit(Task { runSandboxed(function, functionName, parameters) })
        } catch (_: RejectedExecutionException) {
            throw IllegalStateException("JavaScript engine busy")
        }
        try {
            return run.get(MAX_RUN_TIME_NANOS + WATCHDOG_GRACE_NANOS, TimeUnit.NANOSECONDS)
        } catch (_: TimeoutException) {
            // stuck in a native call: the interrupt stops it at its next instruction, if any
            run.cancel(true)
            throw IllegalStateException("JavaScript run time limit exceeded")
        } catch (e: InterruptedException) {
            run.cancel(true)
            Thread.currentThread().interrupt()
            throw IllegalStateException("JavaScript run interrupted", e)
        } catch (e: ExecutionException) {
            when (val cause = e.cause) {
                is RuntimeException -> throw cause
                is Error -> throw cause
                else -> throw IllegalStateException("JavaScript run failed", cause)
            }
        }
    }

    private fun runSandboxed(function: String, functionName: String, parameters: Array<out String>): String {
        try {
            SANDBOX.enterContext().use { context ->
                val scope = context.initSafeStandardObjects()

                // the global scope is "this" of a top-level script, as with evaluateString
                compiled(context, function, functionName).exec(context, scope, scope)
                // the name is not quoted: the code comes from the network
                val jsFunction = scope.get(functionName, scope) as? Function
                    ?: throw IllegalStateException("JavaScript function not found")
                val result = jsFunction.call(context, scope, scope, arrayOf<Any?>(*parameters))
                // measured before a string made of pieces is put together
                if (result is CharSequence && result.length > MAX_RESULT_LENGTH) {
                    throw IllegalStateException("JavaScript result too large")
                }
                return result?.toString()
                    ?: throw IllegalStateException("JavaScript function returned nothing")
            }
        } catch (e: SandboxLimitError) {
            throw IllegalStateException(e.message, e)
        } catch (e: OutOfMemoryError) {
            // a single huge allocation of a native function, which no check can stop before
            // it is made: it failed, so its memory is free again and the app can go on
            throw IllegalStateException("JavaScript memory limit exceeded", e)
        } catch (e: StackOverflowError) {
            throw IllegalStateException("JavaScript nesting too deep", e)
        }
    }

    /**
     * Stops a run that went past its limits. An [Error]: Rhino runs the `finally`
     * blocks of a script for an exception, and one of them could swallow it with a
     * `continue` and loop on; for an error it runs no script code at all.
     */
    private class SandboxLimitError(message: String) : Error(message, null, false, false)

    private fun compiled(context: Context, function: String, functionName: String): Script {
        synchronized(COMPILED_SCRIPTS) { COMPILED_SCRIPTS[function] }?.let { return it }
        // compiled outside the lock: two threads may both compile a new player, which is harmless
        val script = context.compileString(function, functionName, 1, null)
        synchronized(COMPILED_SCRIPTS) { COMPILED_SCRIPTS[function] = script }
        return script
    }

    private fun checkSourceLength(function: String) {
        require(function.length <= MAX_SOURCE_LENGTH) { "JavaScript code too large" }
    }

    private fun usedMemory(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private class SandboxContext(factory: ContextFactory) : Context(factory) {
        /** Set by the first top call of a run: evaluating then calling share the same budget. */
        var started = false
        var deadline = 0L
        var memoryLimit = 0L
    }

    private class SandboxContextFactory : ContextFactory() {
        override fun makeContext(): Context {
            val context = SandboxContext(this)
            // the run time and stack depth limits only work in interpreted mode
            context.setInterpretedMode(true)
            context.setMaximumInterpreterStackDepth(MAX_INTERPRETER_STACK_DEPTH)
            context.setInstructionObserverThreshold(INSTRUCTION_OBSERVER_THRESHOLD)
            // initSafeStandardObjects() leaves out the Java packages; refuse any Java class too
            context.setClassShutter { false }
            return context
        }

        override fun doTopCall(
            callable: Callable,
            context: Context,
            scope: Scriptable,
            thisObj: Scriptable?,
            args: Array<Any?>,
        ): Any? {
            val sandbox = context as SandboxContext
            if (!sandbox.started) {
                sandbox.started = true
                sandbox.deadline = System.nanoTime() + MAX_RUN_TIME_NANOS
                sandbox.memoryLimit = usedMemory() +
                    min(MAX_RUN_MEMORY_BYTES, Runtime.getRuntime().maxMemory() / HEAP_SHARE_DIVISOR)
            }
            return super.doTopCall(callable, context, scope, thisObj, args)
        }

        override fun observeInstructionCount(context: Context, instructionCount: Int) {
            val sandbox = context as SandboxContext
            if (System.nanoTime() - sandbox.deadline > 0 || Thread.currentThread().isInterrupted) {
                throw SandboxLimitError("JavaScript run time limit exceeded")
            }
            val used = usedMemory()
            if (used > sandbox.memoryLimit &&
                used > Runtime.getRuntime().maxMemory() / HEAP_PRESSURE_DIVISOR
            ) {
                throw SandboxLimitError("JavaScript memory limit exceeded")
            }
        }
    }

    /**
     * Sets an external JavaScript interpreter, that is used to run JavaScript code instead of the
     * bundled sandboxed Rhino one.
     *
     * @param javaScriptInterpreter the interpreter to use, or `null` to go back to the
     * bundled one
     */
    @JvmStatic
    public fun setExternalJavaScriptInterpreter(javaScriptInterpreter: JavaScriptInterpreter?) {
        externalJavaScriptInterpreter = javaScriptInterpreter
    }
}
