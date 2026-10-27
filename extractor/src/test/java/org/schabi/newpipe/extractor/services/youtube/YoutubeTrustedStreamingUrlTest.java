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
}
