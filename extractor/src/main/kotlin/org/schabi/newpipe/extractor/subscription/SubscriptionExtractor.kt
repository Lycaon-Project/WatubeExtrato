package org.schabi.newpipe.extractor.subscription

import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.io.IOException
import java.io.InputStream
import java.util.Collections

public abstract class SubscriptionExtractor(
    @JvmField protected val service: StreamingService,
    supportedSources: List<ContentSource>,
) {

    /**
     * Exception that should be thrown when the input **do not** contain valid content that the
     * extractor can parse (e.g. nonexistent user in case of a url extraction).
     */
    public open class InvalidSourceException(detailMessage: String?, cause: Throwable?) :
        ParsingException(
            "Not a valid source" + if (detailMessage == null) "" else " ($detailMessage)",
            cause,
        ) {
        public constructor() : this(null, null)

        public constructor(detailMessage: String?) : this(detailMessage, null)

        public constructor(cause: Throwable?) : this(null, cause)
    }

    public enum class ContentSource {
        CHANNEL_URL,
        INPUT_STREAM,
    }

    public val supportedSources: List<ContentSource> = Collections.unmodifiableList(supportedSources)

    /**
     * Returns an url that can help/guide the user to the file (or channel url) to extract the
     * subscriptions.
     *
     * For example, in YouTube, the export subscriptions url is a good choice to return here.
     */
    public abstract val relatedUrl: String?

    /**
     * Reads and parse a list of [SubscriptionItem] from the given channel url.
     *
     * @throws InvalidSourceException when the channelUrl doesn't exist or is invalid
     */
    @Throws(IOException::class, ExtractionException::class)
    public open fun fromChannelUrl(channelUrl: String?): List<SubscriptionItem> =
        throw UnsupportedOperationException(
            "Service " + service.serviceInfo.name + " doesn't support extracting from a channel url",
        )

    /**
     * Reads and parse a list of [SubscriptionItem] from the given InputStream.
     *
     * @throws InvalidSourceException when the content read from the InputStream is invalid and can
     * not be parsed
     */
    @Throws(ExtractionException::class)
    public open fun fromInputStream(contentInputStream: InputStream): List<SubscriptionItem> =
        throw UnsupportedOperationException(
            "Service " + service.serviceInfo.name + " doesn't support extracting from an InputStream",
        )

    /**
     * Reads and parse a list of [SubscriptionItem] from the given InputStream.
     *
     * @throws InvalidSourceException when the content read from the InputStream is invalid and can
     * not be parsed
     */
    @Throws(ExtractionException::class)
    public open fun fromInputStream(
        contentInputStream: InputStream,
        contentType: String,
    ): List<SubscriptionItem> = throw UnsupportedOperationException(
        "Service " + service.serviceInfo.name + " doesn't support extracting from an InputStream",
    )
}
