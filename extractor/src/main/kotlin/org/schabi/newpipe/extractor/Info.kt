package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.linkhandler.LinkHandler
import org.schabi.newpipe.extractor.utils.ExtractorLogger
import java.io.Serializable

public abstract class Info(
    public val serviceId: Int,
    /**
     * Id of this Info object <br>
     * e.g. Youtube:  https://www.youtube.com/watch?v=RER5qCTzZ7     &gt;    RER5qCTzZ7
     */
    public val id: String,
    /**
     * Different than the [originalUrl] in the sense that it *may* be set as a cleaned
     * url.
     *
     * @see LinkHandler.url
     * @see Extractor.originalUrl
     */
    public val url: String,
    /**
     * The url used to start the extraction of this [Info] object.
     *
     * If you use an api and want to handle the website url, overriding original url is
     * essential.
     *
     * @see Extractor.originalUrl
     */
    public var originalUrl: String,
    public val name: String,
) : Serializable {

    /**
     * Not serialised: stack traces and messages stay out of saved state. Null after
     * deserialisation, which runs no initializer: see [mutableErrors].
     */
    @Transient
    private var errorList: ArrayList<Throwable>? = ArrayList()

    private fun mutableErrors(): ArrayList<Throwable> =
        errorList ?: ArrayList<Throwable>().also { errorList = it }

    init {
        // Neither the URL nor the name: they reveal what the user watches
        ExtractorLogger.d(TAG, "Base Created {}", javaClass.simpleName)
    }

    public constructor(serviceId: Int, linkHandler: LinkHandler, name: String) : this(
        serviceId,
        linkHandler.id,
        linkHandler.url,
        linkHandler.originalUrl,
        name,
    )

    public fun addError(throwable: Throwable) {
        mutableErrors().add(throwable)
    }

    public fun addAllErrors(throwables: Collection<Throwable>) {
        mutableErrors().addAll(throwables)
    }

    /** Without the URLs and the name, which are personal data and would end up in logs. */
    override fun toString(): String = "${javaClass.simpleName}[serviceId=$serviceId]"

    public val service: StreamingService
        get() = try {
            NewPipe.getService(serviceId)
        } catch (e: ExtractionException) {
            // this should be unreachable, as serviceId certainly refers to a valid service
            throw IllegalStateException("Info object has invalid service id", e)
        }

    /** The errors met during the extraction; the list itself, as in the Java API. */
    public val errors: List<Throwable>
        get() = mutableErrors()

    private companion object {
        private const val serialVersionUID: Long = 1L

        private const val TAG = "Info"
    }
}
