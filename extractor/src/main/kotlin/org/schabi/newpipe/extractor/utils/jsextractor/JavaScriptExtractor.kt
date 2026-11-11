package org.schabi.newpipe.extractor.utils.jsextractor

import org.schabi.newpipe.extractor.exceptions.ParsingException

/**
 * Utility class for extracting functions from JavaScript code.
 */
public object JavaScriptExtractor {

    /**
     * Searches the given JavaScript code for the identifier of a function
     * and returns its body.
     *
     * The code (a whole player script, several megabytes) is read in place from the start of
     * the function: only the function itself is copied.
     *
     * @param jsCode JavaScript code
     * @param start start of the function (without the opening brace)
     * @return extracted code (opening brace + function + closing brace)
     * @throws ParsingException if the start is not found or the braces do not match
     */
    @JvmStatic
    @Throws(ParsingException::class)
    public fun matchToClosingBrace(jsCode: String, start: String): String {
        var startIndex = jsCode.indexOf(start)
        if (startIndex < 0) {
            throw ParsingException("Start not found")
        }
        startIndex += start.length

        val lexer = Lexer(jsCode, startIndex)
        var visitedOpenBrace = false

        while (true) {
            val parsedToken = lexer.getNextToken()
            val t = parsedToken.token

            if (t == Token.LC) {
                visitedOpenBrace = true
            } else if (visitedOpenBrace && lexer.isBalanced) {
                return jsCode.substring(startIndex, startIndex + parsedToken.end)
            } else if (t == Token.EOF) {
                throw ParsingException("Could not find matching braces")
            }
        }
    }
}
