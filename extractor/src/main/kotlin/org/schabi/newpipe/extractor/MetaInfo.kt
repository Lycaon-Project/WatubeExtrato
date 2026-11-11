package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.stream.Description
import java.io.Serializable
import java.net.URL

public open class MetaInfo() : Serializable {

    /** Title of the info. Can be empty. */
    public var title: String = ""

    /** Empty until set (the Java API returned `null` before [content] was set). */
    public var content: Description = Description.EMPTY_DESCRIPTION

    /**
     * Never use these [URL]s as keys of a set or a map: `URL.equals` and `URL.hashCode` look the
     * host up in the DNS, telling the resolver which links the user read.
     */
    public var urls: MutableList<URL> = ArrayList()

    public var urlTexts: MutableList<String> = ArrayList()

    public constructor(
        title: String,
        content: Description,
        urls: MutableList<URL>,
        urlTexts: MutableList<String>,
    ) : this() {
        this.title = title
        this.content = content
        this.urls = urls
        this.urlTexts = urlTexts
    }

    public fun addUrl(url: URL) {
        urls.add(url)
    }

    public fun addUrlText(urlText: String) {
        urlTexts.add(urlText)
    }

    private companion object {
        private const val serialVersionUID: Long = 1L
    }
}
