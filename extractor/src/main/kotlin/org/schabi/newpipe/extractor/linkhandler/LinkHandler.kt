package org.schabi.newpipe.extractor.linkhandler

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.Utils
import java.io.Serializable

public open class LinkHandler(
    public val originalUrl: String,
    public val url: String,
    public val id: String,
) : Serializable {

    public constructor(handler: LinkHandler) : this(handler.originalUrl, handler.url, handler.id)

    public val baseUrl: String
        @Throws(ParsingException::class)
        get() = Utils.getBaseUrl(url)

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
