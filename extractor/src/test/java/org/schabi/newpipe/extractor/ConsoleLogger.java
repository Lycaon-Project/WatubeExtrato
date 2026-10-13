package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.utils.Logger;

/**
 * Logger printing the extractor logs to the console, used by the tests only.
 */
public final class ConsoleLogger implements Logger {
    public void debug(final String tag, final String msg) {
        System.out.println("[DEBUG][" + tag + "] " + msg);
    }

    public void debug(final String tag, final String msg, final Throwable throwable) {
        debug(tag, msg);
        throwable.printStackTrace(System.err);
    }
    public void warn(final String tag, final String msg) {
        System.out.println("[WARN ][" + tag + "] " + msg);
    }

    public void warn(final String tag, final String msg, final Throwable t) {
        warn(tag, msg);
        t.printStackTrace(System.err);
    }

    public void error(final String tag, final String msg) {
        System.err.println("[ERROR][" + tag + "] " + msg);
    }

    public void error(final String tag, final String msg, final Throwable t) {
        System.err.println("[ERROR][" + tag + "] " + msg);
        t.printStackTrace(System.err);
    }
}
