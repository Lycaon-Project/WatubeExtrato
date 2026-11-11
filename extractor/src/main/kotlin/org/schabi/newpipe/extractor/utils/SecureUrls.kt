package org.schabi.newpipe.extractor.utils

import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale

/**
 * Keeps every exchange with YouTube, Google and the other services the extractor knows on https:
 * the links it hands over are upgraded, and a plain http request to them is refused before it
 * reaches the downloader, so that nothing about the user is ever sent to them in the clear.
 */
public object SecureUrls {

    private const val HTTP = "http://"
    private const val HTTPS = "https://"
    private const val DELETE = 0x7f

    /** The domains of YouTube and Google, all served over https. */
    private val GOOGLE_DOMAINS = listOf(
        "youtube.com", "youtu.be", "youtube-nocookie.com", "youtubekids.com", "ytimg.com",
        "ggpht.com", "googlevideo.com", "googleusercontent.com", "googleapis.com",
        "google.com", "gstatic.com", "gvt1.com",
    )

    /** The domains of the other services (SoundCloud, Bandcamp, media.ccc.de), all on https. */
    private val SERVICE_DOMAINS = listOf(
        "soundcloud.com", "sndcdn.com", "bandcamp.com", "bcbits.com", "media.ccc.de",
    )

    /**
     * @param host a host name
     * @return whether the host belongs to YouTube or Google
     */
    @JvmStatic
    public fun isGoogleHost(host: String?): Boolean = isUnder(host, GOOGLE_DOMAINS)

    /**
     * @param host a host name
     * @return whether the host belongs to a service only reached over https: YouTube and Google,
     * SoundCloud, Bandcamp or media.ccc.de
     */
    @JvmStatic
    public fun isHttpsOnlyHost(host: String?): Boolean =
        isGoogleHost(host) || isUnder(host, SERVICE_DOMAINS)

    private fun isUnder(host: String?, domains: List<String>): Boolean {
        if (host == null) {
            return false
        }
        // "www.youtube.com." is the same host as "www.youtube.com"
        val name = host.lowercase(Locale.ROOT).trimEnd('.')
        // the domain itself, or a subdomain: "." + domain, without building that string
        return domains.any { domain ->
            name == domain ||
                (
                    name.length > domain.length &&
                        name.endsWith(domain) &&
                        name[name.length - domain.length - 1] == '.'
                    )
        }
    }

    /**
     * Upgrade a plain http link of YouTube or Google to https. Links of other sites are left as
     * they are: nothing tells that they can be reached over https.
     *
     * @param url a link
     * @return the same link, in https when it leads to YouTube or Google
     */
    @JvmStatic
    public fun upgradeToHttps(url: String?): String? {
        if (url == null ||
            !url.regionMatches(0, HTTP, 0, HTTP.length, ignoreCase = true) ||
            !isHttpsOnlyHost(hostOf(url))
        ) {
            return url
        }
        PrivacyEvents.report(PrivacyEvents.Event.HTTPS_UPGRADED)
        return HTTPS + url.substring(HTTP.length)
    }

    /**
     * Refuse a request to YouTube or Google which would not go over https. An URL that cannot
     * be read is refused as well: nothing tells where it would lead.
     *
     * @param url the URL about to be requested
     * @throws IOException if it is a YouTube or Google URL in any other scheme than https
     */
    @JvmStatic
    @Throws(IOException::class)
    public fun checkRequest(url: String) {
        // a backslash or a control character is read differently by java.net.URL and by the
        // HTTP client: the host checked here might not be the one the request reaches
        val ambiguous = url.any { it == '\\' || it < ' ' || it.code == DELETE }
        if (!ambiguous && url.regionMatches(0, HTTPS, 0, HTTPS.length, ignoreCase = true)) {
            return
        }
        val host = hostOf(url)
        if (ambiguous || host == null || isHttpsOnlyHost(host)) {
            PrivacyEvents.report(PrivacyEvents.Event.INSECURE_REQUEST_REFUSED)
            throw IOException("Refusing a request to a known service outside of https")
        }
    }

    /**
     * @param url a link
     * @return whether it is a web link, in http or https with a host: a link in any other scheme
     * (`javascript:`, `intent:`, `file:`, `content:`...) could run code or
     * open local data once followed
     */
    @JvmStatic
    public fun isWebLink(url: String?): Boolean {
        if (url == null) {
            return false
        }
        val web = url.regionMatches(0, HTTPS, 0, HTTPS.length, ignoreCase = true) ||
            url.regionMatches(0, HTTP, 0, HTTP.length, ignoreCase = true)
        val host = if (web) hostOf(url) else null
        return !host.isNullOrEmpty()
    }

    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    private fun hostOf(url: String): String? =
        try {
            URL(url).host
        } catch (e: MalformedURLException) {
            null
        }
}
