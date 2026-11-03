package org.schabi.newpipe.extractor.utils;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

/**
 * Runs JavaScript code taken from a remote page (YouTube's player) in a locked-down Rhino
 * context: interpreted mode, standard objects only, no access to any Java class, and a bounded
 * run time and call depth, so that an unexpected or hostile script can neither reach the
 * application nor hang the thread running it.
 *
 * <p>
 * An application can hand the runs to its own engine with
 * {@link #setExternalJavaScriptInterpreter(JavaScriptInterpreter)}; it then owns the isolation
 * of that engine.
 * </p>
 */
public final class JavaScript {
    @Nullable
    private static volatile JavaScriptInterpreter externalJavaScriptInterpreter;

    private static final long MAX_RUN_TIME_NANOS = TimeUnit.SECONDS.toNanos(5);
    // how many interpreted instructions run between two run time checks
    private static final int INSTRUCTION_OBSERVER_THRESHOLD = 10_000;
    private static final int MAX_INTERPRETER_STACK_DEPTH = 1_000;
    /** The whole player code is a few megabytes; the functions taken from it far less. */
    public static final int MAX_SOURCE_LENGTH = 8 * 1024 * 1024;
    /** A signature or a throttling parameter: a few hundred characters. */
    private static final int MAX_RESULT_LENGTH = 64 * 1024;
    /**
     * How much more of the heap a run may hold: the time limit alone would let a script fill the
     * memory of a phone before it stops it.
     */
    private static final long MAX_RUN_MEMORY_BYTES = 64L * 1024 * 1024;

    private static final ContextFactory SANDBOX = new SandboxContextFactory();

    private JavaScript() {
    }

    public static void compileOrThrow(final String function) {
        checkSourceLength(function);
        try (Context context = SANDBOX.enterContext()) {
            // If it doesn't compile it throws an exception here
            context.compileString(function, null, 1, null);
        } catch (final OutOfMemoryError | StackOverflowError e) {
            // a huge or deeply nested source: refused like any code that does not compile
            throw new IllegalStateException("JavaScript code too complex", e);
        }
    }

    public static String run(final String function,
                             final String functionName,
                             final String... parameters) {
        final JavaScriptInterpreter external = externalJavaScriptInterpreter;
        if (external != null) {
            return external.run(function, functionName, parameters);
        }

        checkSourceLength(function);
        try (Context context = SANDBOX.enterContext()) {
            final ScriptableObject scope = context.initSafeStandardObjects();

            context.evaluateString(scope, function, functionName, 1, null);
            final Function jsFunction = (Function) scope.get(functionName, scope);
            final Object result = jsFunction.call(context, scope, scope, parameters);
            // measured before a string made of pieces is put together
            if (result instanceof CharSequence
                    && ((CharSequence) result).length() > MAX_RESULT_LENGTH) {
                throw new IllegalStateException("JavaScript result too large");
            }
            return result.toString();
        } catch (final SandboxLimitError e) {
            throw new IllegalStateException(e.getMessage(), e);
        } catch (final OutOfMemoryError e) {
            // a single huge allocation of a native function, which no check can stop before
            // it is made: it failed, so its memory is free again and the app can go on
            throw new IllegalStateException("JavaScript memory limit exceeded", e);
        } catch (final StackOverflowError e) {
            throw new IllegalStateException("JavaScript nesting too deep", e);
        }
    }

    /**
     * Stops a run that went past its limits. An {@link Error}: Rhino runs the {@code finally}
     * blocks of a script for an exception, and one of them could swallow it with a
     * {@code continue} and loop on; for an error it runs no script code at all.
     */
    private static final class SandboxLimitError extends Error {
        SandboxLimitError(final String message) {
            super(message, null, false, false);
        }
    }

    private static void checkSourceLength(final String function) {
        if (function.length() > MAX_SOURCE_LENGTH) {
            throw new IllegalArgumentException("JavaScript code too large");
        }
    }

    private static long usedMemory() {
        final Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static final class SandboxContext extends Context {
        /** Set by the first top call of a run: evaluating then calling share the same budget. */
        private boolean started;
        private long deadline;
        private long memoryLimit;

        SandboxContext(final ContextFactory factory) {
            super(factory);
        }
    }

    private static final class SandboxContextFactory extends ContextFactory {
        @Override
        protected Context makeContext() {
            final Context context = new SandboxContext(this);
            // the run time and stack depth limits only work in interpreted mode
            context.setInterpretedMode(true);
            context.setMaximumInterpreterStackDepth(MAX_INTERPRETER_STACK_DEPTH);
            context.setInstructionObserverThreshold(INSTRUCTION_OBSERVER_THRESHOLD);
            // initSafeStandardObjects() leaves out the Java packages; refuse any Java class too
            context.setClassShutter(className -> false);
            return context;
        }

        @Override
        protected Object doTopCall(final Callable callable,
                                   final Context context,
                                   final Scriptable scope,
                                   final Scriptable thisObj,
                                   final Object[] args) {
            final SandboxContext sandbox = (SandboxContext) context;
            if (!sandbox.started) {
                sandbox.started = true;
                sandbox.deadline = System.nanoTime() + MAX_RUN_TIME_NANOS;
                // a quarter of the heap at most, for the small heaps of some phones
                sandbox.memoryLimit = usedMemory()
                        + Math.min(MAX_RUN_MEMORY_BYTES, Runtime.getRuntime().maxMemory() / 4);
            }
            return super.doTopCall(callable, context, scope, thisObj, args);
        }

        @Override
        protected void observeInstructionCount(final Context context,
                                               final int instructionCount) {
            final SandboxContext sandbox = (SandboxContext) context;
            if (System.nanoTime() - sandbox.deadline > 0) {
                throw new SandboxLimitError("JavaScript run time limit exceeded");
            }
            if (usedMemory() > sandbox.memoryLimit) {
                throw new SandboxLimitError("JavaScript memory limit exceeded");
            }
        }
    }

    /**
     * Sets an external JavaScript interpreter, that is used to run JavaScript code instead of the
     * bundled sandboxed Rhino one.
     *
     * @param javaScriptInterpreter the interpreter to use, or {@code null} to go back to the
     *                              bundled one
     */
    public static void setExternalJavaScriptInterpreter(
            @Nullable final JavaScriptInterpreter javaScriptInterpreter) {
        JavaScript.externalJavaScriptInterpreter = javaScriptInterpreter;
    }
}
