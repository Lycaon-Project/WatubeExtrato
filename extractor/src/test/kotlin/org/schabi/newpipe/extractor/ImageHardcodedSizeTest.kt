/*
 * SPDX-FileCopyrightText: 2026 NewPipe Extractor contributors <https://newpipe.net>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package org.schabi.newpipe.extractor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale
import java.util.regex.Pattern

/**
 * Ensures that hard-coded image sizes in the extractor source are known,
 * documented, and stay correct.
 *
 * Some extractors hard-code image dimensions when a service guarantees them
 * (e.g. the Bandcamp radio button is `512x512`). When a service changes such a size, the
 * extractor would silently report wrong values — that's what happened in
 * [#1525](https://github.com/TeamNewPipe/NewPipeExtractor/pull/1525).
 *
 * This test scans the source code for every `new Image(url, w, h, ...)`
 * call with hard-coded numeric dimensions and asserts that each occurrence is
 * explicitly registered in [WHITELISTED_SIZES]. Adding a new hard-coded
 * size therefore requires documenting it here and (when possible) verifying it,
 * instead of it silently rotting.
 *
 * See [issue #1526](https://github.com/TeamNewPipe/NewPipeExtractor/issues/1526).
 */
class ImageHardcodedSizeTest {

    /** One hard-coded `new Image(...)` size found in the source code. */
    private class HardcodedSize(val filePath: String, val size: String, val urlArgument: String)

    @Test
    @Throws(IOException::class)
    fun everyHardcodedImageSizeMustBeWhitelisted() {
        val hardcodedSizes = findHardcodedSizes()
        assertTrue(
            hardcodedSizes.isNotEmpty(),
            "Scanner found no hardcoded image sizes; the scanner is probably broken",
        )

        val unknown = StringBuilder()
        for (hardcodedSize in hardcodedSizes) {
            val allowed = WHITELISTED_SIZES[hardcodedSize.filePath]
            val allowedUrlFragment = allowed?.get(hardcodedSize.size)

            if (allowedUrlFragment == null) {
                unknown.append(hardcodedSize.filePath)
                    .append(": ").append(hardcodedSize.size)
                    .append("\n")
            } else if (allowedUrlFragment.isNotEmpty() &&
                !hardcodedSize.urlArgument.contains(allowedUrlFragment)
            ) {
                // Cross-check: the size is supposed to be encoded in the image URL
                unknown.append(hardcodedSize.filePath)
                    .append(": hardcoded size ").append(hardcodedSize.size)
                    .append(" does not match URL fragment \"")
                    .append(allowedUrlFragment)
                    .append("\" (URL: ").append(hardcodedSize.urlArgument)
                    .append(")\n")
            }
        }

        assertEquals(
            "",
            unknown.toString(),
            "Found hard-coded image sizes which are not whitelisted (and possibly not" +
                " verified). If a service guarantees these sizes, add them to" +
                " WHITELISTED_SIZES with a justification and, when possible, a" +
                " URL fragment that must contain the size; otherwise use" +
                " Image.WIDTH_UNKNOWN / Image.HEIGHT_UNKNOWN.",
        )
    }

    private companion object {
        val IMAGE_CONSTRUCTOR: Pattern = Pattern.compile("new Image\\(")

        /** Kotlin has no `new`: a call is `Image(` not preceded by a name part. */
        val KOTLIN_IMAGE_CONSTRUCTOR: Pattern = Pattern.compile("(?<![A-Za-z0-9_.])Image\\(")

        /**
         * The hard-coded sizes which are currently known and accepted.
         *
         * Keys are file name suffixes (relative to `extractor/src/main/java` or
         * `extractor/src/main/kotlin`),
         * values map the size (`<width>x<height>`) to an optional URL fragment
         * which the corresponding image URL must contain (used to cross-check that
         * the hard-coded value matches the URL a service uses for that size).
         */
        val WHITELISTED_SIZES: Map<String, Map<String, String>> = mapOf(
            // Bandcamp's radio button PNG literally encodes its size in its file name
            "org/schabi/newpipe/extractor/services/bandcamp/extractors/" +
                "BandcampRadioStreamExtractor.kt" to mapOf("512x512" to "-512.png"),
        )

        @Throws(IOException::class)
        fun findHardcodedSizes(): List<HardcodedSize> {
            val result = ArrayList<HardcodedSize>()
            for (language in arrayOf("java", "kotlin")) {
                val sourceRoot = resolveMainSourceDirectory(language) ?: continue
                Files.walk(sourceRoot).use { files ->
                    files.filter { path ->
                        path.toString().endsWith(".java") || path.toString().endsWith(".kt")
                    }.forEach { path -> findHardcodedSizesInFile(sourceRoot, path, result) }
                }
            }
            return result
        }

        fun findHardcodedSizesInFile(
            sourceRoot: Path,
            path: Path,
            result: MutableList<HardcodedSize>,
        ) {
            val relativePath = sourceRoot.relativize(path).toString().replace('\\', '/')
            val content = try {
                Files.readString(path, StandardCharsets.UTF_8)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
            val constructor =
                if (relativePath.endsWith(".kt")) KOTLIN_IMAGE_CONSTRUCTOR else IMAGE_CONSTRUCTOR
            findHardcodedSizesInContent(relativePath, content, constructor, result)
        }

        /** The `java` or `kotlin` main source directory, or null when there is none. */
        fun resolveMainSourceDirectory(language: String): Path? {
            // Gradle runs tests from the module directory, but support running from
            // the repository root too (e.g. IDEs with a different working directory)
            for (candidate in arrayOf("src/main/$language", "extractor/src/main/$language")) {
                val path = Paths.get(candidate).toAbsolutePath()
                if (Files.isDirectory(path)) {
                    return path
                }
            }
            return null
        }

        /**
         * Finds `new Image(url, width, height, resolutionLevel)` calls whose
         * width and height are hard-coded numeric literals.
         */
        fun findHardcodedSizesInContent(
            filePath: String,
            content: String,
            constructor: Pattern,
            result: MutableList<HardcodedSize>,
        ) {
            val matcher = constructor.matcher(content)
            while (matcher.find()) {
                val args = extractArguments(content, matcher.end() - 1) ?: continue
                val arguments = splitTopLevelArguments(args)
                if (arguments.size == 4 &&
                    arguments[1].matches(Regex("\\d+")) &&
                    arguments[2].matches(Regex("\\d+"))
                ) {
                    val size = String.format(
                        Locale.ENGLISH,
                        "%sx%s",
                        arguments[1].toInt(),
                        arguments[2].toInt(),
                    )
                    result.add(HardcodedSize(filePath, size, arguments[0]))
                }
            }
        }

        /** Returns the balanced-parentheses argument string starting at `openParenIndex`. */
        fun extractArguments(content: String, openParenIndex: Int): String? {
            var depth = 0
            var inString = false
            var escaped = false
            for (i in openParenIndex until content.length) {
                val c = content[i]
                if (escaped) {
                    escaped = false
                } else if (inString && c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = !inString
                } else if (!inString) {
                    if (c == '(') {
                        depth++
                    } else if (c == ')') {
                        depth--
                        if (depth == 0) {
                            return content.substring(openParenIndex + 1, i)
                        }
                    }
                }
            }
            return null // unbalanced (e.g. commented-out code) — ignore
        }

        /** Splits a constructor argument string on top-level commas only. */
        fun splitTopLevelArguments(args: String): List<String> {
            val arguments = ArrayList<String>()
            val current = StringBuilder()
            var depth = 0
            var inString = false
            var escaped = false
            for (c in args) {
                if (escaped) {
                    current.append(c)
                    escaped = false
                } else if (inString && c == '\\') {
                    current.append(c)
                    escaped = true
                } else if (c == '"') {
                    current.append(c)
                    inString = !inString
                } else if (inString) {
                    current.append(c)
                } else if (c == '(' || c == '[') {
                    depth++
                    current.append(c)
                } else if (c == ')' || c == ']') {
                    depth--
                    current.append(c)
                } else if (c == ',' && depth == 0) {
                    arguments.add(current.toString().trim())
                    current.setLength(0)
                } else {
                    current.append(c)
                }
            }
            // a Kotlin trailing comma leaves only blanks after the last argument
            if (current.toString().isNotBlank()) {
                arguments.add(current.toString().trim())
            }
            return arguments
        }
    }
}
