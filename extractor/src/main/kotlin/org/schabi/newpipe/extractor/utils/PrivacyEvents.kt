package org.schabi.newpipe.extractor.utils

/**
 * Tells the application what the extractor did to protect its user, so that it can show it:
 * how many trackers it removed, which addresses it refused to reach. Only the kind of each event
 * is reported, never the link, address or video it was about.
 */
public object PrivacyEvents {

    /** What the extractor did. */
    public enum class Event {
        /** A tracking parameter removed from a link, or a YouTube redirect link bypassed. */
        TRACKER_REMOVED,

        /** A streaming URL dropped because it did not lead to YouTube's video servers. */
        STREAM_URL_REFUSED,

        /** A redirect of a video server to an address outside of them, not followed. */
        REDIRECT_BLOCKED,

        /** A YouTube or Google link given in plain http, handed over in https. */
        HTTPS_UPGRADED,

        /** A request to YouTube or Google in plain http, refused before it went out. */
        INSECURE_REQUEST_REFUSED,
    }

    /** Receives the events, on the thread of the extraction that caused them. */
    public fun interface Listener {
        public fun onPrivacyEvent(event: Event)
    }

    @Volatile
    private var listener: Listener? = null

    /**
     * @param newListener the listener of the events, or `null` to stop reporting them
     */
    @JvmStatic
    public fun setListener(newListener: Listener?) {
        listener = newListener
    }

    /**
     * @param event what the extractor just did
     */
    @JvmStatic
    public fun report(event: Event) {
        val current = listener ?: return
        try {
            current.onPrivacyEvent(event)
        } catch (e: RuntimeException) {
            // a count that could not be kept never stops the extraction that reported it
            ExtractorLogger.e("PrivacyEvents", "Privacy event listener failed", e)
        }
    }
}
