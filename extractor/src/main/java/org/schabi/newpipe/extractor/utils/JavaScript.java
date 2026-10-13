package org.schabi.newpipe.extractor.utils;

import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.util.concurrent.TimeUnit;

/**
 * Runs JavaScript code taken from a remote page (YouTube's player) in a locked-down Rhino
 * context: interpreted mode, standard objects only, no access to any Java class, and a bounded
 * run time and call depth, so that an unexpected or hostile script can neither reach the
 * application nor hang the thread running it.
 */
public final class JavaScript {

    private static final long MAX_RUN_TIME_NANOS = TimeUnit.SECONDS.toNanos(5);
    // how many interpreted instructions run between two run time checks
    private static final int INSTRUCTION_OBSERVER_THRESHOLD = 10_000;
    private static final int MAX_INTERPRETER_STACK_DEPTH = 1_000;

    private static final ContextFactory SANDBOX = new SandboxContextFactory();

    private JavaScript() {
    }

    public static void compileOrThrow(final String function) {
        try (Context context = SANDBOX.enterContext()) {
            // If it doesn't compile it throws an exception here
            context.compileString(function, null, 1, null);
        }
    }

    public static String run(final String function,
                             final String functionName,
                             final String... parameters) {
        try (Context context = SANDBOX.enterContext()) {
            final ScriptableObject scope = context.initSafeStandardObjects();

            context.evaluateString(scope, function, functionName, 1, null);
            final Function jsFunction = (Function) scope.get(functionName, scope);
            final Object result = jsFunction.call(context, scope, scope, parameters);
            return result.toString();
        }
    }

    private static final class SandboxContext extends Context {
        private long deadline;

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
            ((SandboxContext) context).deadline = System.nanoTime() + MAX_RUN_TIME_NANOS;
            return super.doTopCall(callable, context, scope, thisObj, args);
        }

        @Override
        protected void observeInstructionCount(final Context context,
                                               final int instructionCount) {
            // a RuntimeException other than Rhino's own cannot be caught by the script
            if (System.nanoTime() - ((SandboxContext) context).deadline > 0) {
                throw new IllegalStateException("JavaScript run time limit exceeded");
            }
        }
    }
}
