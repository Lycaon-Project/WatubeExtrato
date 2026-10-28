package org.schabi.newpipe.extractor.utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Tells the application what the extractor did to protect its user, so that it can show it:
 * how many trackers it removed, which addresses it refused to reach. Only the kind of each event
 * is reported, never the link, address or video it was about.
 */
public final class PrivacyEvents {

    /** What the extractor did. */
    public enum Event {
        /** A tracking parameter removed from a link, or a YouTube redirect link bypassed. */
        TRACKER_REMOVED,
        /** A streaming URL dropped because it did not lead to YouTube's video servers. */
        STREAM_URL_REFUSED,
        /** A redirect of a video server to an address outside of them, not followed. */
        REDIRECT_BLOCKED,
        /** A YouTube or Google link given in plain http, handed over in https. */
        HTTPS_UPGRADED,
        /** A request to YouTube or Google in plain http, refused before it went out. */
        INSECURE_REQUEST_REFUSED
    }

    /** Receives the events, on the thread of the extraction that caused them. */
    public interface Listener {
        void onPrivacyEvent(@Nonnull Event event);
    }

    @Nullable
    private static volatile Listener listener;

    private PrivacyEvents() {
    }

    /**
     * @param newListener the listener of the events, or {@code null} to stop reporting them
     */
    public static void setListener(@Nullable final Listener newListener) {
        listener = newListener;
    }

    /**
     * @param event what the extractor just did
     */
    public static void report(@Nonnull final Event event) {
        final Listener current = listener;
        if (current != null) {
            current.onPrivacyEvent(event);
        }
    }
}
