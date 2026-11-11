package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.MessageDigest

internal class ClearUrlsTest {

    @Test
    fun compilingTheRulesOfASiteIsNotCountedInTheMatchingBudget() {
        val deadline = ClearUrls.Deadline()
        // longer than the whole budget: the first link of a site must still be cleaned
        deadline.excluding { Thread.sleep(300) }

        assertTrue(Regex("utm_source").containsMatchIn(deadline.view("https://a.example/?utm_source=x")))
    }

    @Test
    fun rulesAreTheReviewedPinnedRelease() {
        // Changing the rules is a deliberate act: update RULES_SHA256 and the version in
        // ClearUrls only after reviewing the diff of a release published 7 days ago or more.
        val stream = ClearUrls::class.java.getResourceAsStream(ClearUrls.RULES_RESOURCE)
        assertNotNull(stream, "the rules are missing from the extractor")
        val bytes = checkNotNull(stream).use { it.readBytes() }
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        assertEquals(ClearUrls.RULES_SHA256, digest)
    }

    @Test
    fun everyRuleCompilesWithJavaRegexes() {
        val stats = ClearUrls.compileAllForTest()
        assertEquals(206, stats.providers, "providers")
        assertEquals(0, stats.invalidPatterns, "patterns that Java cannot compile")
        assertTrue(stats.patterns > 1_000, "patterns: ${stats.patterns}")
    }
}
