package org.schabi.newpipe.extractor.exceptions

import org.schabi.newpipe.extractor.downloader.Response

public open class HttpResponseException : ExtractionException {

    /**
     * Only the status code is kept: the URL (which may carry IDs or tokens) and the status text
     * chosen by the server never reach the message, hence logs and crash reports.
     */
    public constructor(response: Response) : this("HTTP error " + response.responseCode())

    public constructor(message: String?) : super(message)
}
