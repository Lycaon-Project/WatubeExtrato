package org.schabi.newpipe.extractor.exceptions

public open class SoundCloudGoPlusContentException : ContentNotAvailableException {
    public constructor() : super(MESSAGE)

    public constructor(cause: Throwable?) : super(MESSAGE, cause)

    private companion object {
        private const val MESSAGE = "This track is a SoundCloud Go+ track"
    }
}
