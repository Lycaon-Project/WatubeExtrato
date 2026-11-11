package org.schabi.newpipe.extractor.exceptions

public open class YoutubeMusicPremiumContentException : ContentNotAvailableException {
    public constructor() : super(MESSAGE)

    public constructor(cause: Throwable?) : super(MESSAGE, cause)

    private companion object {
        private const val MESSAGE = "This video is a YouTube Music Premium video"
    }
}
