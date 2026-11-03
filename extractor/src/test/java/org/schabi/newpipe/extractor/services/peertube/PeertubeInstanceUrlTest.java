package org.schabi.newpipe.extractor.services.peertube;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * An instance is only reached in plain http when it runs on the device itself: a host which only
 * starts like "localhost" is somewhere else, and stays on https.
 */
class PeertubeInstanceUrlTest {

    @Test
    void httpsInstancesAreAccepted() {
        assertDoesNotThrow(() -> new PeertubeInstance("https://framatube.org"));
    }

    @Test
    void plainHttpIsOnlyForTheDevice() {
        assertDoesNotThrow(() -> new PeertubeInstance("http://localhost:9000"));
        assertDoesNotThrow(() -> new PeertubeInstance("http://127.0.0.1"));
        assertDoesNotThrow(() -> new PeertubeInstance("http://[::1]:9000"));
    }

    @Test
    void lookalikeLocalHostsAreRefused() {
        assertThrows(IllegalArgumentException.class,
                () -> new PeertubeInstance("http://localhost.example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new PeertubeInstance("http://127.0.0.1.example.com"));
        assertThrows(IllegalArgumentException.class,
                () -> new PeertubeInstance("http://example.com"));
    }
}
