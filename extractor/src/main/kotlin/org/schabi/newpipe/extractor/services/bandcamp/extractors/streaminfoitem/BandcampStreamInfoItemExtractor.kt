package org.schabi.newpipe.extractor.services.bandcamp.extractors.streaminfoitem

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.localization.DateWrapper
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor
import org.schabi.newpipe.extractor.stream.StreamType

/**
 * Implements methods that return a constant value in subclasses for better readability.
 */
public abstract class BandcampStreamInfoItemExtractor(override val uploaderUrl: String?) :
    StreamInfoItemExtractor {

    override val streamType: StreamType
        get() = StreamType.AUDIO_STREAM

    override val viewCount: Long
        get() = -1

    override val textualUploadDate: String?
        get() = null

    override val uploadDate: DateWrapper?
        get() = null

    @get:Throws(ParsingException::class)
    override val isUploaderVerified: Boolean
        get() = false

    override val isAd: Boolean
        get() = false
}
