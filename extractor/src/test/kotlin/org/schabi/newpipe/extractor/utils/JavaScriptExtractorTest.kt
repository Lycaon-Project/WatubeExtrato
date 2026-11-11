package org.schabi.newpipe.extractor.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.FileUtils.resolveTestResource
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.jsextractor.JavaScriptExtractor
import org.schabi.newpipe.extractor.utils.jsextractor.Lexer
import org.schabi.newpipe.extractor.utils.jsextractor.Token
import java.io.IOException
import java.nio.file.Files

internal class JavaScriptExtractorTest {
    @Test
    @Throws(ParsingException::class)
    fun testJsExtractor() {
        val src = "Wka=function(d){var x = [/,,/,913,/(,)}/g,\"abcdef}\\\"\",];var y = 10/2/1;" +
            "return x[1][y];}//some={}random-padding+;"
        val result = JavaScriptExtractor.matchToClosingBrace(src, "Wka=function")
        assertEquals(
            "(d){var x = [/,,/,913,/(,)}/g,\"abcdef}\\\"\",];var y = 10/2/1;return x[1][y];}",
            result,
        )
    }

    @Test
    @Throws(ParsingException::class, IOException::class)
    fun testEverythingJs() {
        val jsFile = resolveTestResource("es5.js")
        val contentBuilder = StringBuilder()
        Files.lines(jsFile.toPath()).use { lines ->
            lines.forEach { line -> contentBuilder.append(line).append("\n") }
        }

        val js = contentBuilder.toString()

        val lexer = Lexer(js)
        var parsedToken: Lexer.ParsedToken? = null

        try {
            while (true) {
                parsedToken = lexer.getNextToken()
                if (parsedToken.token == Token.EOF) {
                    break
                }
            }
        } catch (e: Exception) {
            if (parsedToken != null) {
                throw ParsingException(
                    "Issue occured at pos ${parsedToken.end}, after\n" +
                        js.substring(maxOf(0, parsedToken.start - 50), parsedToken.end),
                    e,
                )
            }
            throw e
        }

        assertTrue(lexer.isBalanced)
    }
}
