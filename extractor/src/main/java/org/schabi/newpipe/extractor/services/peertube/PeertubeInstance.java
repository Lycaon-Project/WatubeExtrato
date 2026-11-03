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
import java.util.Locale;

public class PeertubeInstance {

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
        final boolean local = lower.startsWith("http://localhost")
                || lower.startsWith("http://127.0.0.1");
        if (!lower.startsWith("https://") && !local) {
            throw new IllegalArgumentException("PeerTube instances are only reached over https");
        }
        return url;
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
