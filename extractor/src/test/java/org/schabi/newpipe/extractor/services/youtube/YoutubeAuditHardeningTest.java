package org.schabi.newpipe.extractor.services.youtube;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.util.concurrent.atomic.AtomicReference;

/**
 * What YouTube sends is never trusted as it is: a long player does not overflow the stack, the
 * throttling parameter cannot rewrite the rest of a streaming URL, links never leave the web, and
 * text handed over as HTML is always escaped.
 */
class YoutubeAuditHardeningTest {

    /** The stack of a small worker thread. */
    private static final long WORKER_STACK_BYTES = 256 * 1024;

    private static JsonObject json(final String text) throws JsonParserException {
        return JsonParser.object().from(text);
    }

    private static JsonObject redirectTo(final String url) {
        final JsonObject urlEndpoint = new JsonObject();
        urlEndpoint.put("url", url);
        final JsonObject endpoint = new JsonObject();
        endpoint.put("urlEndpoint", urlEndpoint);
        return endpoint;
    }

    // Player code

    @Test
    void aLongHelperObjectIsFoundOnASmallStack() throws InterruptedException {
        // long, on the small stack of a worker thread, and with Windows line ends, which the
        // former pattern (?>.|\n) could not cross
        final String body = "ab:function(a,b){a.splice(0,b)},\r\n".repeat(40_000);
        final String player = "x=1;var Xy={" + body + "cd:function(a){a.reverse()}};y=2;";
        final AtomicReference<Object> result = new AtomicReference<>();

        final Thread worker = new Thread(null, () -> {
            try {
                result.set(YoutubeSignatureUtils.getHelperObject(player, "Xy"));
            } catch (final ParsingException | StackOverflowError e) {
                result.set(e);
            }
        }, "small-stack", WORKER_STACK_BYTES);
        worker.start();
        worker.join();

        final String helper = assertInstanceOf(String.class, result.get());
        assertTrue(helper.startsWith("var Xy={"));
        assertTrue(helper.endsWith("}};"));
    }

    @Test
    void onlyTheThrottlingParameterIsReplaced() {
        final String url = "https://rr1.googlevideo.com/videoplayback?id=abc&lsig=abc&n=abc&x=1";

        assertEquals("https://rr1.googlevideo.com/videoplayback?id=abc&lsig=abc&n=XYZ&x=1",
                YoutubeThrottlingParameterUtils.replaceThrottlingParameter(url, "XYZ"));
        assertEquals("https://rr1.googlevideo.com/videoplayback?id=1",
                YoutubeThrottlingParameterUtils.replaceThrottlingParameter(
                        "https://rr1.googlevideo.com/videoplayback?id=1", "XYZ"));
    }

    @Test
    void aThrottlingParameterMustBeUrlSafe() {
        assertTrue(YoutubeThrottlingParameterUtils.isValidThrottlingParameter("aB3_-x"));
        assertFalse(YoutubeThrottlingParameterUtils.isValidThrottlingParameter("a&host=evil"));
        assertFalse(YoutubeThrottlingParameterUtils.isValidThrottlingParameter("a/b"));
        assertFalse(YoutubeThrottlingParameterUtils.isValidThrottlingParameter(""));
    }

    // Links

    @Test
    void aRedirectToAnotherSchemeGivesNoLink() {
        assertNull(YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("https://www.youtube.com/redirect?event=x&q=javascript%3Aalert(1)")));
        assertNull(YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("/redirect?q=intent%3A%2F%2Fevil%23Intent%3Bend")));
        assertNull(YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("/redirect?q=file%3A%2F%2F%2Fsdcard%2Fx")));
        assertNull(YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("httpfoo:bar")));
    }

    @Test
    void everyYoutubeRedirectPageIsSkipped() {
        for (final String redirect : new String[]{
                "https://www.youtube.com/redirect?q=https%3A%2F%2Fexample.org%2Fa",
                "https://m.youtube.com/redirect?event=x&q=https%3A%2F%2Fexample.org%2Fa",
                "http://youtube.com/redirect?q=https%3A%2F%2Fexample.org%2Fa",
                "/redirect?q=https%3A%2F%2Fexample.org%2Fa"}) {
            assertEquals("https://example.org/a",
                    YoutubeParsingHelper.getUrlFromNavigationEndpoint(redirectTo(redirect)),
                    redirect);
        }
    }

    @Test
    void googleHostsAreMatchedExactly() {
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://www.google.com/search?q=a"));
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://google.co.uk/"));
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://m.google.fr/"));
        assertFalse(YoutubeParsingHelper.isGoogleURL("https://google.example.com/"));
        assertFalse(YoutubeParsingHelper.isGoogleURL("https://www.google.evil.example/"));
    }

    @Test
    void aCacheLinkWithoutItsTargetIsKept() {
        final String url = "https://webcache.googleusercontent.com/search?q=x";
        assertEquals(url, assertDoesNotThrow(
                () -> YoutubeParsingHelper.extractCachedUrlIfNeeded(url)));
        assertEquals("https://example.org/", YoutubeParsingHelper.extractCachedUrlIfNeeded(
                "https://webcache.googleusercontent.com/search?q=cache:https://example.org/"));
    }

    // Text handed over as HTML

    @Test
    void runsAreEscapedInHtml() throws JsonParserException {
        final JsonObject text = json("{\"runs\":[{\"text\":\"<img src=x onerror=alert(1)>\"},"
                + "{\"text\":\" & <b>\",\"bold\":true}]}");

        assertEquals("&lt;img src=x onerror=alert(1)&gt;<b> &amp; &lt;b&gt;</b>",
                YoutubeParsingHelper.getTextFromObject(text, true));
        // as plain text, nothing changes
        assertEquals("<img src=x onerror=alert(1)> & <b>",
                YoutubeParsingHelper.getTextFromObject(text, false));
    }

    @Test
    void simpleTextIsEscapedInHtml() throws JsonParserException {
        final JsonObject text = json("{\"simpleText\":\"<script>x</script>\\nnext\"}");

        assertEquals("&lt;script&gt;x&lt;/script&gt;<br>next",
                YoutubeParsingHelper.getTextFromObject(text, true));
        assertEquals("<script>x</script>\nnext",
                YoutubeParsingHelper.getTextFromObject(text, false));
    }

    @Test
    void runsBeyondTheDescriptionAreClosedAtItsEnd() throws JsonParserException {
        final JsonObject description = json("{\"content\":\"hello world\",\"styleRuns\":["
                + "{\"startIndex\":6,\"length\":2147483647,\"italic\":true},"
                + "{\"startIndex\":50,\"length\":3,\"italic\":true}]}");

        final String html = assertDoesNotThrow(
                () -> YoutubeDescriptionHelper.attributedDescriptionToHtml(description));
        assertEquals("hello <i>world</i>", html);
    }

    @Test
    void invalidAudioTrackTagsAreIgnored() {
        assertNull(assertDoesNotThrow(
                () -> YoutubeParsingHelper.extractAudioTrackType("not base64 !!!")));
    }
}
