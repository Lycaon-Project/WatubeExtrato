package org.schabi.newpipe.extractor.services.youtube

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import java.util.concurrent.atomic.AtomicReference

/**
 * What YouTube sends is never trusted as it is: a long player does not overflow the stack, the
 * throttling parameter cannot rewrite the rest of a streaming URL, links never leave the web, and
 * text handed over as HTML is always escaped.
 */
internal class YoutubeAuditHardeningTest {

    private fun json(text: String): JsonObject = JsonParser.`object`().from(text)

    private fun redirectTo(url: String): JsonObject {
        val urlEndpoint = JsonObject()
        urlEndpoint["url"] = url
        val endpoint = JsonObject()
        endpoint["urlEndpoint"] = urlEndpoint
        return endpoint
    }

    // Meta info

    @Test
    fun infoPanelParagraphsAreEscaped() {
        val contents = json(
            """{"c":[{"itemSectionRenderer":{"contents":[{"infoPanelContentRenderer":{"paragraphs":[""" +
                """{"runs":[{"text":"<img src=x onerror=alert(1)>"}]},{"simpleText":"a & b"}]}}]}}]}""",
        ).getArray("c")
        val content = YoutubeMetaInfoHelper.getMetaInfo(contents).single().content.content
        assertFalse(content.contains("<img"), content)
        assertTrue(content.contains("&lt;img"), content)
        assertTrue(content.contains("<br>"), content)
    }

    // Player code

    @Test
    fun aLongHelperObjectIsFoundOnASmallStack() {
        // long, on the small stack of a worker thread, and with Windows line ends, which the
        // former pattern (?>.|\n) could not cross
        val body = "ab:function(a,b){a.splice(0,b)},\r\n".repeat(40_000)
        val player = "x=1;var Xy={" + body + "cd:function(a){a.reverse()}};y=2;"
        val result = AtomicReference<Any>()

        val worker = Thread(null, {
            try {
                result.set(YoutubeSignatureUtils.getHelperObject(player, "Xy"))
            } catch (e: ParsingException) {
                result.set(e)
            } catch (e: StackOverflowError) {
                result.set(e)
            }
        }, "small-stack", WORKER_STACK_BYTES)
        worker.start()
        worker.join()

        val helper = assertInstanceOf(String::class.java, result.get())
        assertTrue(helper.startsWith("var Xy={"))
        assertTrue(helper.endsWith("}};"))
    }

    @Test
    fun onlyTheThrottlingParameterIsReplaced() {
        val url = "https://rr1.googlevideo.com/videoplayback?id=abc&lsig=abc&n=abc&x=1"

        assertEquals(
            "https://rr1.googlevideo.com/videoplayback?id=abc&lsig=abc&n=XYZ&x=1",
            YoutubeThrottlingParameterUtils.replaceThrottlingParameter(url, "XYZ")
        )
        assertEquals(
            "https://rr1.googlevideo.com/videoplayback?id=1",
            YoutubeThrottlingParameterUtils.replaceThrottlingParameter(
                "https://rr1.googlevideo.com/videoplayback?id=1", "XYZ"
            )
        )
    }

    @Test
    fun aThrottlingParameterMustBeUrlSafe() {
        assertTrue(YoutubeThrottlingParameterUtils.isValidThrottlingParameter("aB3_-x"))
        assertFalse(YoutubeThrottlingParameterUtils.isValidThrottlingParameter("a&host=evil"))
        assertFalse(YoutubeThrottlingParameterUtils.isValidThrottlingParameter("a/b"))
        assertFalse(YoutubeThrottlingParameterUtils.isValidThrottlingParameter(""))
    }

    // Links

    @Test
    fun aRedirectToAnotherSchemeGivesNoLink() {
        assertNull(
            YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("https://www.youtube.com/redirect?event=x&q=javascript%3Aalert(1)")
            )
        )
        assertNull(
            YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("/redirect?q=intent%3A%2F%2Fevil%23Intent%3Bend")
            )
        )
        assertNull(
            YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                redirectTo("/redirect?q=file%3A%2F%2F%2Fsdcard%2Fx")
            )
        )
        assertNull(YoutubeParsingHelper.getUrlFromNavigationEndpoint(redirectTo("httpfoo:bar")))
    }

    @Test
    fun everyYoutubeRedirectPageIsSkipped() {
        for (redirect in arrayOf(
            "https://www.youtube.com/redirect?q=https%3A%2F%2Fexample.org%2Fa",
            "https://m.youtube.com/redirect?event=x&q=https%3A%2F%2Fexample.org%2Fa",
            "http://youtube.com/redirect?q=https%3A%2F%2Fexample.org%2Fa",
            "/redirect?q=https%3A%2F%2Fexample.org%2Fa"
        )) {
            assertEquals(
                "https://example.org/a",
                YoutubeParsingHelper.getUrlFromNavigationEndpoint(redirectTo(redirect)),
                redirect
            )
        }
    }

    @Test
    fun googleHostsAreMatchedExactly() {
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://www.google.com/search?q=a"))
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://google.co.uk/"))
        assertTrue(YoutubeParsingHelper.isGoogleURL("https://m.google.fr/"))
        assertFalse(YoutubeParsingHelper.isGoogleURL("https://google.example.com/"))
        assertFalse(YoutubeParsingHelper.isGoogleURL("https://www.google.evil.example/"))
    }

    @Test
    fun aCacheLinkWithoutItsTargetIsKept() {
        val url = "https://webcache.googleusercontent.com/search?q=x"
        assertEquals(url, assertDoesNotThrow<String?> { YoutubeParsingHelper.extractCachedUrlIfNeeded(url) })
        assertEquals(
            "https://example.org/",
            YoutubeParsingHelper.extractCachedUrlIfNeeded(
                "https://webcache.googleusercontent.com/search?q=cache:https://example.org/"
            )
        )
    }

    // Text handed over as HTML

    @Test
    fun runsAreEscapedInHtml() {
        val text = json(
            "{\"runs\":[{\"text\":\"<img src=x onerror=alert(1)>\"}," +
                "{\"text\":\" & <b>\",\"bold\":true}]}"
        )

        assertEquals(
            "&lt;img src=x onerror=alert(1)&gt;<b> &amp; &lt;b&gt;</b>",
            YoutubeParsingHelper.getTextFromObject(text, true)
        )
        // as plain text, nothing changes
        assertEquals(
            "<img src=x onerror=alert(1)> & <b>",
            YoutubeParsingHelper.getTextFromObject(text, false)
        )
    }

    @Test
    fun simpleTextIsEscapedInHtml() {
        val text = json("{\"simpleText\":\"<script>x</script>\\nnext\"}")

        assertEquals(
            "&lt;script&gt;x&lt;/script&gt;<br>next",
            YoutubeParsingHelper.getTextFromObject(text, true)
        )
        assertEquals(
            "<script>x</script>\nnext",
            YoutubeParsingHelper.getTextFromObject(text, false)
        )
    }

    @Test
    fun runsBeyondTheDescriptionAreClosedAtItsEnd() {
        val description = json(
            "{\"content\":\"hello world\",\"styleRuns\":[" +
                "{\"startIndex\":6,\"length\":2147483647,\"italic\":true}," +
                "{\"startIndex\":50,\"length\":3,\"italic\":true}]}"
        )

        val html = assertDoesNotThrow<String?> { YoutubeDescriptionHelper.attributedDescriptionToHtml(description) }
        assertEquals("hello <i>world</i>", html)
    }

    @Test
    fun invalidAudioTrackTagsAreIgnored() {
        assertNull(
            assertDoesNotThrow<Any?> { YoutubeParsingHelper.extractAudioTrackType("not base64 !!!") }
        )
    }

    private companion object {
        /** The stack of a small worker thread. */
        const val WORKER_STACK_BYTES = 256L * 1024
    }
}
