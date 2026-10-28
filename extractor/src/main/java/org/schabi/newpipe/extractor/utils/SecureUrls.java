package org.schabi.newpipe.extractor.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Keeps every exchange with YouTube and Google on https: the links the extractor hands over are
 * upgraded, and a plain http request to them is refused before it reaches the downloader, so
 * that nothing about the user is ever sent to them in the clear.
 */
public final class SecureUrls {

    private static final String HTTP = "http://";
    private static final String HTTPS = "https://";

    /** The domains of YouTube and Google, all served over https. */
    private static final List<String> GOOGLE_DOMAINS = List.of(
            "youtube.com", "youtu.be", "youtube-nocookie.com", "ytimg.com", "ggpht.com",
            "googlevideo.com", "googleusercontent.com", "googleapis.com", "google.com");

    private SecureUrls() {
    }

    /**
     * @param host a host name
     * @return whether the host belongs to YouTube or Google
     */
    public static boolean isGoogleHost(@Nullable final String host) {
        if (host == null) {
            return false;
        }
        final String name = host.toLowerCase(Locale.ROOT);
        for (final String domain : GOOGLE_DOMAINS) {
            if (name.equals(domain) || name.endsWith("." + domain)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Upgrade a plain http link of YouTube or Google to https. Links of other sites are left as
     * they are: nothing tells that they can be reached over https.
     *
     * @param url a link
     * @return the same link, in https when it leads to YouTube or Google
     */
    @Nullable
    public static String upgradeToHttps(@Nullable final String url) {
        if (url == null || !url.regionMatches(true, 0, HTTP, 0, HTTP.length())
                || !isGoogleHost(hostOf(url))) {
            return url;
        }
        PrivacyEvents.report(PrivacyEvents.Event.HTTPS_UPGRADED);
        return HTTPS + url.substring(HTTP.length());
    }

    /**
     * Refuse a request to YouTube or Google which would not go over https. An URL that cannot
     * be read is refused as well: nothing tells where it would lead.
     *
     * @param url the URL about to be requested
     * @throws IOException if it is a YouTube or Google URL in any other scheme than https
     */
    public static void checkRequest(@Nonnull final String url) throws IOException {
        if (url.regionMatches(true, 0, HTTPS, 0, HTTPS.length())) {
            return;
        }
        final String host = hostOf(url);
        if (host == null || isGoogleHost(host)) {
            PrivacyEvents.report(PrivacyEvents.Event.INSECURE_REQUEST_REFUSED);
            throw new IOException("Refusing a request to YouTube or Google outside of https");
        }
    }

    @Nullable
    private static String hostOf(@Nonnull final String url) {
        try {
            return new URL(url).getHost();
        } catch (final MalformedURLException e) {
            return null;
        }
    }
}
