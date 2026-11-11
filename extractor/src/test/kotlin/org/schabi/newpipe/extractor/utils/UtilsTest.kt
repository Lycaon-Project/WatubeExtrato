package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.exceptions.ParsingException

internal class UtilsTest {
    @Test
    @Throws(ParsingException::class)
    fun testMixedNumberWordToLong() {
        assertEquals(10L, Utils.mixedNumberWordToLong("10"))
        assertEquals(10.5e3, Utils.mixedNumberWordToLong("10.5K").toDouble(), 0.0)
        assertEquals(10.5e6, Utils.mixedNumberWordToLong("10.5M").toDouble(), 0.0)
        assertEquals(10.5e6, Utils.mixedNumberWordToLong("10,5M").toDouble(), 0.0)
        assertEquals(1.5e9, Utils.mixedNumberWordToLong("1,5B").toDouble(), 0.0)
    }

    @Test
    fun testJoin() {
        assertEquals(
            "some,random,not-null,stuff",
            Utils.nonEmptyAndNullJoin(",", "some", "null", "random", "", "not-null", null, "stuff"),
        )
    }

    @Test
    @Throws(ParsingException::class)
    fun testGetBaseUrl() {
        assertEquals(
            "https://www.youtube.com",
            Utils.getBaseUrl("https://www.youtube.com/watch?v=Hu80uDzh8RY"),
        )
        assertEquals(
            "vnd.youtube",
            Utils.getBaseUrl("vnd.youtube://www.youtube.com/watch?v=jZViOEv90dI"),
        )
        assertEquals("vnd.youtube", Utils.getBaseUrl("vnd.youtube:jZViOEv90dI"))
        assertEquals("vnd.youtube", Utils.getBaseUrl("vnd.youtube://n8X9_MgEdCg"))
        assertEquals(
            "https://music.youtube.com",
            Utils.getBaseUrl("https://music.youtube.com/watch?v=O0EDx9WAelc"),
        )
    }

    @Test
    fun googleRedirectsOfOtherHostsOrToOtherSchemesAreNotFollowed() {
        listOf(
            "https://evilgoogle.com/url?sa=t&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DHu80uDzh8RY",
            "https://google.com.evil.example/url?url=https%3A%2F%2Fwww.youtube.com%2F",
            "https://www.google.com/url?sa=t&url=javascript%3Aalert(1)",
            "https://www.google.com/url?sa=t&url=file%3A%2F%2F%2Fetc%2Fpasswd",
        ).forEach { assertEquals(it, Utils.followGoogleRedirectIfNeeded(it)) }
        assertEquals(
            "https://www.youtube.com/watch?v=Hu80uDzh8RY",
            Utils.followGoogleRedirectIfNeeded(
                "https://www.google.co.uk/url?sa=t&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DHu80uDzh8RY",
            ),
        )
    }

    @Test
    fun testFollowGoogleRedirect() {
        assertEquals(
            "https://www.youtube.com/watch?v=Hu80uDzh8RY",
            Utils.followGoogleRedirectIfNeeded(
                "https://www.google.it/url?sa=t&rct=j&q=&esrc=s&cd=&cad=rja&uact=8" +
                    "&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DHu80uDzh8RY&source=video",
            ),
        )
        assertEquals(
            "https://www.youtube.com/watch?v=0b6cFWG45kA",
            Utils.followGoogleRedirectIfNeeded(
                "https://www.google.com/url?sa=t&rct=j&q=&esrc=s&source=video&cd=&cad=rja&uact=8" +
                    "&url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3D0b6cFWG45kA",
            ),
        )
        assertEquals(
            "https://soundcloud.com/ciaoproduction",
            Utils.followGoogleRedirectIfNeeded(
                "https://www.google.com/url?sa=t&url=https%3A%2F%2Fsoundcloud.com%2Fciaoproduction" +
                    "&rct=j&q=&esrc=s&source=web&cd=",
            ),
        )

        assertEquals(
            "https://www.youtube.com/watch?v=Hu80uDzh8RY&param=xyz",
            Utils.followGoogleRedirectIfNeeded("https://www.youtube.com/watch?v=Hu80uDzh8RY&param=xyz"),
        )
        assertEquals(
            "https://www.youtube.com/watch?v=Hu80uDzh8RY&url=hello",
            Utils.followGoogleRedirectIfNeeded("https://www.youtube.com/watch?v=Hu80uDzh8RY&url=hello"),
        )
    }
}
