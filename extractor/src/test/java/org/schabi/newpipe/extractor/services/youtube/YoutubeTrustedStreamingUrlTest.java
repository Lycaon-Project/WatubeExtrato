package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isTrustedStreamingUrl;

class YoutubeTrustedStreamingUrlTest {
    @Test
    void videoServersOverHttpsAreTrusted() {
        assertTrue(isTrustedStreamingUrl(
                "https://rr3---sn-25ge7nsk.googlevideo.com/videoplayback?expire=1&itag=18"));
        assertTrue(isTrustedStreamingUrl(
                "https://manifest.googlevideo.com/api/manifest/hls_variant/expire/1/file/index.m3u8"));
    }

    @Test
    void anythingElseIsRefused() {
        assertFalse(isTrustedStreamingUrl(null));
        assertFalse(isTrustedStreamingUrl(""));
        assertFalse(isTrustedStreamingUrl("http://rr3---sn-25ge7nsk.googlevideo.com/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://evil.example/videoplayback?x=googlevideo.com"));
        assertFalse(isTrustedStreamingUrl("https://googlevideo.com.evil.example/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://evilgooglevideo.com/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://user@rr3---sn.googlevideo.com/videoplayback"));
        assertFalse(isTrustedStreamingUrl("sabr://18"));
    }

    @Test
    void theHttpsPortOnly() {
        assertTrue(isTrustedStreamingUrl("https://rr3---sn-25ge7nsk.googlevideo.com:443/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://rr3---sn-25ge7nsk.googlevideo.com:8443/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://rr3---sn-25ge7nsk.googlevideo.com:80/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://rr3---sn-25ge7nsk.googlevideo.com:0/videoplayback"));
    }

    @Test
    void theCaseOfTheUrlDoesNotMatter() {
        assertTrue(isTrustedStreamingUrl("HTTPS://RR3---SN-25GE7NSK.GOOGLEVIDEO.COM/videoplayback"));
    }

    @Test
    void onlyAPlainHostNameIsTrusted() {
        // trailing dot, empty label
        assertFalse(isTrustedStreamingUrl("https://rr3---sn-25ge7nsk.googlevideo.com./videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://rr3..googlevideo.com/videoplayback"));
        // percent-encoded, once or twice: a decoder could turn it into another host
        assertFalse(isTrustedStreamingUrl("https://evil.example%2F.googlevideo.com/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://evil.example%252F.googlevideo.com/videoplayback"));
        // look-alike letters (a Cyrillic "о"), raw or as punycode outside googlevideo.com
        assertFalse(isTrustedStreamingUrl("https://rr3.googlevideо.com/videoplayback"));
        assertFalse(isTrustedStreamingUrl("https://rr3.xn--googlevide-6ij.com/videoplayback"));
        // an ideographic full stop, which some decoders read as a dot
        assertFalse(isTrustedStreamingUrl("https://rr3.googlevideo.com。evil.example/videoplayback"));
    }
}
