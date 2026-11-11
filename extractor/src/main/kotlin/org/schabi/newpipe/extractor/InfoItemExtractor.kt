package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.exceptions.ParsingException

public interface InfoItemExtractor {
    @get:Throws(ParsingException::class)
    public val name: String

    @get:Throws(ParsingException::class)
    public val url: String

    @get:Throws(ParsingException::class)
    public val thumbnails: List<Image>
}
