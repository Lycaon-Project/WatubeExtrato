package org.schabi.newpipe.extractor.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Removes the tracking parameters that services and advertisers append to links, the way the
 * query parameter stripping of Firefox based privacy browsers (LibreWolf, IronFox) does, before
 * a link found in a description or a comment is handed to the user.
 *
 * <p>
 * Only parameters that identify a click, a campaign or a sharer are removed: the ones a page
 * needs to show its content are kept, so that every link keeps working.
 * </p>
 */
public final class UrlPrivacy {

    /** Parameters which track the click on any site. */
    private static final Set<String> TRACKING_PARAMETERS = Set.of(
            "fbclid", "gclid", "gclsrc", "dclid", "gbraid", "wbraid", "msclkid", "yclid",
            "twclid", "ttclid", "li_fat_id", "igshid", "mc_cid", "mc_eid", "_hsenc", "_hsmi",
            "mkt_tok", "oly_anon_id", "oly_enc_id", "rb_clickid", "vero_id", "wickedid");

    private static final String CAMPAIGN_PREFIX = "utm_";

    /**
     * Parameters which only track on the sites of their owner: elsewhere the same names can
     * carry real data.
     */
    private static final Map<String, Set<String>> SITE_TRACKING_PARAMETERS = Map.of(
            "youtube.com", Set.of("si", "pp", "feature", "embeds_referring_euri",
                    "source_ve_path"),
            "youtu.be", Set.of("si", "feature"),
            "spotify.com", Set.of("si", "context"),
            "instagram.com", Set.of("igsh", "img_index"));

    private UrlPrivacy() {
    }

    /**
     * Remove the tracking parameters of a link.
     *
     * @param url the link, which is returned unchanged when it cannot be parsed
     * @return the same link without its tracking parameters
     */
    @Nullable
    public static String removeTrackingParameters(@Nullable final String url) {
        if (url == null) {
            return null;
        }
        final int queryStart = url.indexOf('?');
        if (queryStart < 0) {
            return url;
        }
        final int fragmentStart = url.indexOf('#', queryStart);
        final int queryEnd = fragmentStart < 0 ? url.length() : fragmentStart;

        final Set<String> siteParameters = siteParameters(url);
        final List<String> kept = new ArrayList<>();
        for (final String parameter : url.substring(queryStart + 1, queryEnd).split("&")) {
            if (parameter.isEmpty()) {
                continue;
            }
            if (isTracking(parameter, siteParameters)) {
                PrivacyEvents.report(PrivacyEvents.Event.TRACKER_REMOVED);
            } else {
                kept.add(parameter);
            }
        }

        final StringBuilder cleaned = new StringBuilder(url.substring(0, queryStart));
        if (!kept.isEmpty()) {
            cleaned.append('?').append(String.join("&", kept));
        }
        return cleaned.append(url.substring(queryEnd)).toString();
    }

    private static boolean isTracking(@Nonnull final String parameter,
                                      @Nonnull final Set<String> siteParameters) {
        final int equals = parameter.indexOf('=');
        final String rawName = equals < 0 ? parameter : parameter.substring(0, equals);
        String name;
        try {
            name = Utils.decodeUrlUtf8(rawName);
        } catch (final IllegalArgumentException e) {
            // a malformed escape: compare the raw name
            name = rawName;
        }
        name = name.toLowerCase(Locale.ROOT);
        return name.startsWith(CAMPAIGN_PREFIX) || TRACKING_PARAMETERS.contains(name)
                || siteParameters.contains(name);
    }

    @Nonnull
    private static Set<String> siteParameters(@Nonnull final String url) {
        final String host;
        try {
            host = new URL(url).getHost().toLowerCase(Locale.ROOT);
        } catch (final MalformedURLException e) {
            return Set.of();
        }
        for (final Map.Entry<String, Set<String>> site : SITE_TRACKING_PARAMETERS.entrySet()) {
            if (host.equals(site.getKey()) || host.endsWith("." + site.getKey())) {
                return site.getValue();
            }
        }
        return Set.of();
    }
}
