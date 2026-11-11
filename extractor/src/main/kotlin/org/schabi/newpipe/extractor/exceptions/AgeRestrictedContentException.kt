package org.schabi.newpipe.extractor.exceptions

public open class AgeRestrictedContentException : ContentNotAvailableException {
    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)
}
