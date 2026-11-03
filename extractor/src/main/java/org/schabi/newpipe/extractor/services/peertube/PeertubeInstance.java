package org.schabi.newpipe.extractor.services.peertube;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.utils.JsonUtils;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

public class PeertubeInstance {

    /** The only hosts reached in plain http: the device itself. */
    private static final Set<String> LOCAL_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]");

    private final String url;
    private String name;
    public static final PeertubeInstance DEFAULT_INSTANCE
            = new PeertubeInstance("https://framatube.org", "FramaTube");

    public PeertubeInstance(final String url) {
        this(url, "PeerTube");
    }

    /**
     * @param url  the address of the instance: https, or plain http on the device itself only
     * @param name its name
     * @throws IllegalArgumentException for an instance that would be reached in the clear
     */
    public PeertubeInstance(final String url, final String name) {
        this.url = checkUrl(url);
        this.name = name;
    }

    /** Every request to the instance would tell what the user watches: never in the clear. */
    private static String checkUrl(final String url) {
        final String lower = url.toLowerCase(Locale.ROOT);
        // the host itself, not a prefix of it: http://localhost.example.com is not on the device
        final String host = hostOf(url);
        final boolean local = lower.startsWith("http://") && host != null
                && LOCAL_HOSTS.contains(host);
        if (!lower.startsWith("https://") && !local) {
            throw new IllegalArgumentException("PeerTube instances are only reached over https");
        }
        return url;
    }

    @Nullable
    private static String hostOf(final String url) {
        try {
            final String host = new URI(url).getHost();
            return host == null ? null : host.toLowerCase(Locale.ROOT);
        } catch (final URISyntaxException e) {
            return null;
        }
    }

    public String getUrl() {
        return url;
    }

    public void fetchInstanceMetaData() throws Exception {
        final Response response;
        try {
            response = NewPipe.getDownloader().get(url + "/api/v1/config");
        } catch (ReCaptchaException | IOException e) {
            throw new Exception("unable to configure instance " + url, e);
        }

        if (response == null || Utils.isBlank(response.responseBody())) {
            throw new Exception("unable to configure instance " + url);
        }

        try {
            final JsonObject json = JsonParser.object().from(response.responseBody());
            this.name = JsonUtils.getString(json, "instance.name");
        } catch (JsonParserException | ParsingException e) {
            throw new Exception("unable to parse instance config", e);
        }
    }

    public String getName() {
        return name;
    }

}
