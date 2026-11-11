package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.utils.Utils
import java.io.Serializable

/**
 * Where the next page of a list is. Pages hold URLs, IDs, cookies and request bodies, so they
 * keep the default [toString], which shows none of them.
 */
public open class Page(
    public val url: String?,
    public val id: String?,
    public val ids: List<String>?,
    public val cookies: Map<String, String>?,
    public val body: ByteArray?,
) : Serializable {

    public constructor(url: String?) : this(url, null, null, null, null)

    public constructor(url: String?, id: String?) : this(url, id, null, null, null)

    public constructor(url: String?, id: String?, body: ByteArray?) : this(url, id, null, null, body)

    public constructor(url: String?, body: ByteArray?) : this(url, null, null, null, body)

    public constructor(url: String?, cookies: Map<String, String>?) :
        this(url, null, null, cookies, null)

    public constructor(ids: List<String>?) : this(null, null, ids, null, null)

    public constructor(ids: List<String>?, cookies: Map<String, String>?) :
        this(null, null, ids, cookies, null)

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmStatic
        public fun isValid(page: Page?): Boolean =
            page != null && (!Utils.isNullOrEmpty(page.url) || !Utils.isNullOrEmpty(page.ids))
    }
}
