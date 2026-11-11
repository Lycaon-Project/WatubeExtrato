package org.schabi.newpipe.extractor.playlist

import org.schabi.newpipe.extractor.InfoItemsCollector
import org.schabi.newpipe.extractor.exceptions.ParsingException

public open class PlaylistInfoItemsCollector(serviceId: Int) :
    InfoItemsCollector<PlaylistInfoItem, PlaylistInfoItemExtractor>(serviceId) {

    @Throws(ParsingException::class)
    override fun extract(extractor: PlaylistInfoItemExtractor): PlaylistInfoItem {
        val resultItem = PlaylistInfoItem(serviceId, extractor.url, extractor.name)

        collect { resultItem.uploaderName = extractor.uploaderName }
        collect { resultItem.uploaderUrl = extractor.uploaderUrl }
        collect { resultItem.isUploaderVerified = extractor.isUploaderVerified }
        collect { resultItem.thumbnails = extractor.thumbnails }
        collect { resultItem.streamCount = extractor.streamCount }
        collect { resultItem.description = extractor.description }
        collect { resultItem.playlistType = extractor.playlistType }
        return resultItem
    }

    /** Runs one optional extraction: a failure is recorded, not thrown. */
    private inline fun collect(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            addError(e)
        }
    }
}
