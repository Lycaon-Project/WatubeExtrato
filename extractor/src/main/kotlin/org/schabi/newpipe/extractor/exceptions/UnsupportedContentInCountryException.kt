package org.schabi.newpipe.extractor.exceptions

/**
 * Exception for contents not supported in a country.
 *
 * Unsupported content means content is not intentionally geographically restricted such as for
 * distribution rights, for which [GeographicRestrictionException] should be used instead.
 */
public open class UnsupportedContentInCountryException : ContentNotAvailableException {
    public constructor(message: String?) : super(message)

    public constructor(message: String?, cause: Throwable?) : super(message, cause)
}
