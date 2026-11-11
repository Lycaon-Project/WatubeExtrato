package org.schabi.newpipe.extractor.services.peertube

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable

/**
 * An instance is only reached in plain http when it runs on the device itself: a host which only
 * starts like "localhost" is somewhere else, and stays on https.
 */
class PeertubeInstanceUrlTest {

    @Test
    fun httpsInstancesAreAccepted() {
        assertDoesNotThrow(Executable { PeertubeInstance("https://framatube.org") })
    }

    @Test
    fun plainHttpIsOnlyForTheDevice() {
        assertDoesNotThrow(Executable { PeertubeInstance("http://localhost:9000") })
        assertDoesNotThrow(Executable { PeertubeInstance("http://127.0.0.1") })
        assertDoesNotThrow(Executable { PeertubeInstance("http://[::1]:9000") })
    }

    @Test
    fun lookalikeLocalHostsAreRefused() {
        assertThrows(
            IllegalArgumentException::class.java,
            Executable { PeertubeInstance("http://localhost.example.com") }
        )
        assertThrows(
            IllegalArgumentException::class.java,
            Executable { PeertubeInstance("http://127.0.0.1.example.com") }
        )
        assertThrows(
            IllegalArgumentException::class.java,
            Executable { PeertubeInstance("http://example.com") }
        )
    }
}
