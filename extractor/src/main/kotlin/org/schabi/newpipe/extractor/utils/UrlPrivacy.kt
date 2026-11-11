package org.schabi.newpipe.extractor.utils

import java.net.MalformedURLException
import java.net.URL
import java.util.Locale

/**
 * Removes the tracking parameters that services and advertisers append to links, the way the
 * query parameter stripping of Firefox based privacy browsers (LibreWolf, IronFox) does, before
 * a link found in a description or a comment is handed to the user.
 *
 * Only parameters that identify a click, a campaign or a sharer are removed: the ones a page
 * needs to show its content are kept, so that every link keeps working.
 */
public object UrlPrivacy {

    /** Parameters which track the click on any site. */
    private val TRACKING_PARAMETERS = setOf(
        "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
        "twclid", "ttclid", "li_fat_id", "igshid", "mc_cid", "mc_eid", "_hsenc", "_hsmi",
        "mkt_tok", "oly_anon_id", "oly_enc_id", "rb_clickid", "vero_id", "wickedid",
    )

    private const val CAMPAIGN_PREFIX = "utm_"

    /**
     * Parameters which only track on the sites of their owner: elsewhere the same names can
     * carry real data.
     */
    private val SITE_TRACKING_PARAMETERS = mapOf(
        "youtube.com" to setOf("si", "pp", "feature", "embeds_referring_euri", "source_ve_path"),
        "youtu.be" to setOf("si", "feature"),
        "spotify.com" to setOf("si", "context"),
        "instagram.com" to setOf("igsh", "img_index"),
    )

    /** How many redirections [cleanLink] unwraps, at most, before it keeps the link it got to. */
    private const val MAX_REDIRECTIONS = 3

    /**
     * Remove the tracking parameters of a link: the ones above, then the ones of the ClearURLs
     * rules ([ClearUrls]). The scheme and the host of the link never change, so that the
     * host an app shows is the one the link opens.
     *
     * @param url the link, which is returned unchanged when it cannot be parsed
     * @return the same link without its tracking parameters
     */
    @JvmStatic
    public fun removeTrackingParameters(url: String?): String? {
        if (url == null) {
            return null
        }
        val cleaned = ClearUrls.removeTrackers(removeBuiltInParameters(url))
        repeat(cleaned.removed) { PrivacyEvents.report(PrivacyEvents.Event.TRACKER_REMOVED) }
        return cleaned.url
    }

    /**
     * Skip the redirection pages of known sites (Google, Facebook, Instagram, Reddit, LinkedIn,
     * ad servers...), offline, then remove the tracking parameters of the link reached.
     *
     * Without it, a click goes through the server of the redirection, which records the address
     * of the device and the click before sending it on. At most [MAX_REDIRECTIONS] nested
     * redirections are unwrapped, and only towards a web link.
     *
     * Unlike [removeTrackingParameters], the host can change: show the host of the result.
     *
     * @param url the link
     * @return where the link leads, without its tracking parameters
     */
    @JvmStatic
    public fun cleanLink(url: String?): String? {
        if (url == null) {
            return null
        }
        var current: String = url
        for (i in 0 until MAX_REDIRECTIONS) {
            current = ClearUrls.redirectTarget(current) ?: break
            PrivacyEvents.report(PrivacyEvents.Event.TRACKER_REMOVED)
        }
        return removeTrackingParameters(current)
    }

    private fun removeBuiltInParameters(url: String): String {
        // a "?" after the "#" belongs to the fragment (the route of a web app), not to the query
        val fragmentStart = url.indexOf('#')
        val queryStart = url.indexOf('?')
        if (queryStart < 0 || (fragmentStart in 0 until queryStart)) {
            return url
        }
        val queryEnd = if (fragmentStart < 0) url.length else fragmentStart

        val siteParameters = siteParameters(url)
        val kept = ArrayList<String>()
        for (parameter in url.substring(queryStart + 1, queryEnd).split('&')) {
            if (parameter.isEmpty()) {
                continue
            }
            if (isTracking(parameter, siteParameters)) {
                PrivacyEvents.report(PrivacyEvents.Event.TRACKER_REMOVED)
            } else {
                kept.add(parameter)
            }
        }

        return buildString(url.length) {
            append(url, 0, queryStart)
            if (kept.isNotEmpty()) {
                append('?')
                kept.joinTo(this, "&")
            }
            append(url, queryEnd, url.length)
        }
    }

    private fun isTracking(parameter: String, siteParameters: Set<String>): Boolean {
        val equals = parameter.indexOf('=')
        val rawName = if (equals < 0) parameter else parameter.substring(0, equals)
        val decoded = try {
            Utils.decodeUrlUtf8(rawName)
        } catch (e: IllegalArgumentException) {
            // a malformed escape: compare the raw name
            rawName
        }
        val name = decoded.lowercase(Locale.ROOT)
        return name.startsWith(CAMPAIGN_PREFIX) ||
            name in TRACKING_PARAMETERS ||
            name in siteParameters
    }

    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    private fun siteParameters(url: String): Set<String> {
        val host = try {
            URL(url).host.lowercase(Locale.ROOT)
        } catch (e: MalformedURLException) {
            return emptySet()
        }
        for ((site, parameters) in SITE_TRACKING_PARAMETERS) {
            if (host == site || host.endsWith(".$site")) {
                return parameters
            }
        }
        return emptySet()
    }
}
