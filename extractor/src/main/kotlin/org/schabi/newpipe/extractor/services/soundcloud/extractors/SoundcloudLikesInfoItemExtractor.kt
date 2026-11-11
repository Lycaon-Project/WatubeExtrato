package org.schabi.newpipe.extractor.services.soundcloud.extractors

import org.schabi.newpipe.extractor.json.JsonObject

public open class SoundcloudLikesInfoItemExtractor(itemObject: JsonObject) :
    SoundcloudStreamInfoItemExtractor(itemObject.getObject("track"))
