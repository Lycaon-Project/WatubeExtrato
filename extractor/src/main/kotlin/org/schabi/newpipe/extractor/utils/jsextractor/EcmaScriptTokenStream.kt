/*
 * Source: Mozilla Rhino, org.mozilla.javascript.TokenStream
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 *
 */
package org.schabi.newpipe.extractor.utils.jsextractor

import org.mozilla.javascript.Kit
import org.mozilla.javascript.ScriptRuntime
import org.schabi.newpipe.extractor.exceptions.ParsingException

/**
 * Based on Mozilla Rhino's (v1.7.14) org.mozilla.javascript.TokenStream
 *
 * Changes:
 * - Tailored for [Lexer]
 * - Removed all not needed code to improve performance
 * - Optimized for ECMAScript6/2015
 * - Reads the source in place from [startOffset], so that a function can be lexed out of a
 *   whole player script without copying the rest of it
 * - Error messages never quote the source (it comes from the network)
 *
 * @param sourceString the source code
 * @param startOffset where to start reading [sourceString]; positions are relative to it
 * @param lineno the number of the first line
 * @param strictMode whether the code is read in strict mode
 */
internal class EcmaScriptTokenStream(
    private val sourceString: String,
    startOffset: Int,
    lineno: Int,
    private val strictMode: Boolean,
) {
    // stuff other than whitespace since start of line
    private var dirtyLine = false

    private var stringBuffer = CharArray(INITIAL_BUFFER_SIZE)
    private var stringBufferTop = 0

    // Room to backtrace from to < on failed match of the last - in <!--
    private val ungetBuffer = IntArray(UNGET_BUFFER_SIZE)
    private var ungetCursor = 0

    private var lineEndChar = -1

    var lineno: Int = lineno
        private set

    // sourceCursor is an index into the source string
    private var sourceCursor = startOffset

    // cursor is a monotonically increasing index into the original
    // source stream, tracking exactly how far scanning has progressed.
    // Its value is the index of the next character to be scanned.
    private var cursor = 0

    // Record start and end positions of last scanned token.
    var tokenBeg: Int = 0
        private set
    var tokenEnd: Int = 0
        private set

    @Throws(ParsingException::class)
    fun nextToken(): Token {
        var tt = getToken()
        while (tt == Token.EOL || tt == Token.COMMENT) {
            tt = getToken()
        }
        return tt
    }

    @Throws(ParsingException::class)
    private fun getToken(): Token {
        var c: Int

        // Eat whitespace, possibly sensitive to newlines.
        while (true) {
            c = getChar()
            if (c == EOF_CHAR) {
                tokenBeg = cursor - 1
                tokenEnd = cursor
                return Token.EOF
            } else if (c == NEWLINE) {
                dirtyLine = false
                tokenBeg = cursor - 1
                tokenEnd = cursor
                return Token.EOL
            } else if (!isJSSpace(c)) {
                if (c != '-'.code) {
                    dirtyLine = true
                }
                break
            }
        }

        // Assume the token will be 1 char - fixed up below.
        tokenBeg = cursor - 1
        tokenEnd = cursor

        // identifier/keyword/instanceof?
        // watch out for starting with a <backslash>
        val identifierStart: Boolean
        var isUnicodeEscapeStart = false
        if (c == BACKSLASH) {
            c = getChar()
            if (c == 'u'.code) {
                identifierStart = true
                isUnicodeEscapeStart = true
                stringBufferTop = 0
            } else {
                identifierStart = false
                ungetChar(c)
                c = BACKSLASH
            }
        } else {
            identifierStart = Character.isJavaIdentifierStart(c.toChar())
            if (identifierStart) {
                stringBufferTop = 0
                addToString(c)
            }
        }

        if (identifierStart) {
            return readIdentifier(isUnicodeEscapeStart)
        }

        // is it a number?
        if (isDigit(c) || (c == '.'.code && isDigit(peekChar()))) {
            return readNumber(c)
        }

        // is it a string or template literal?
        if (c == '"'.code || c == '\''.code || c == '`'.code) {
            return readString(c)
        }

        return readPunctuator(c)
    }

    @Throws(ParsingException::class)
    private fun readIdentifier(startsWithEscape: Boolean): Token {
        var isUnicodeEscapeStart = startsWithEscape
        var containsEscape = startsWithEscape
        var c: Int
        while (true) {
            if (isUnicodeEscapeStart) {
                // strictly speaking we should probably push-back
                // all the bad characters if the <backslash>uXXXX
                // sequence is malformed. But since there isn't a
                // correct context(is there?) for a bad Unicode
                // escape sequence in an identifier, we can report
                // an error here.
                addToString(readIdentifierUnicodeEscape())
                isUnicodeEscapeStart = false
                continue
            }
            c = getChar()
            if (c == BACKSLASH) {
                c = getChar()
                if (c != 'u'.code) {
                    throw ParsingException("illegal character in JavaScript code")
                }
                isUnicodeEscapeStart = true
                containsEscape = true
                continue
            }
            if (c == EOF_CHAR ||
                c == BYTE_ORDER_MARK ||
                !Character.isJavaIdentifierPart(c.toChar())
            ) {
                break
            }
            addToString(c)
        }
        ungetChar(c)

        val str = getStringFromBuffer()
        if (!containsEscape) {
            // OPT we shouldn't have to make a string (object!) to
            // check if it's a keyword.

            // Return the corresponding token if it's a keyword
            val result = stringToKeywordForES(str, strictMode)
            if (result != Token.EOF) {
                return result // Always needed due to ECMAScript
            }
        }
        return Token.NAME
    }

    /** Reads the four hex digits of a `\uXXXX` escape in an identifier. */
    @Throws(ParsingException::class)
    private fun readIdentifierUnicodeEscape(): Int {
        var escapeVal = 0
        var i = 0
        while (i != UNICODE_ESCAPE_DIGITS) {
            escapeVal = Kit.xDigitToInt(getChar(), escapeVal)
            // Next check takes care about c < 0 and bad escape
            if (escapeVal < 0) {
                throw ParsingException("invalid unicode escape")
            }
            i++
        }
        return escapeVal
    }

    @Throws(ParsingException::class)
    private fun readNumber(first: Int): Token {
        var c = first
        stringBufferTop = 0
        var base = 10
        var isOldOctal = false

        if (c == '0'.code) {
            c = getChar()
            if (c == 'x'.code || c == 'X'.code) {
                base = 16
                c = getChar()
            } else if (c == 'o'.code || c == 'O'.code) {
                base = 8
                c = getChar()
            } else if (c == 'b'.code || c == 'B'.code) {
                base = 2
                c = getChar()
            } else if (isDigit(c)) {
                base = 8
                isOldOctal = true
            } else {
                addToString('0'.code)
            }
        }

        val emptyDetector = stringBufferTop
        if (!isOldOctal) {
            // base 10, 16, 2 or (new) 8
            c = readDigitsOrThrow(base, c)
        } else {
            while (isDigit(c)) {
                // finally the oldOctal case
                if (c >= '8'.code) {
                    /*
                     * We permit 08 and 09 as decimal numbers, which
                     * makes our behavior a superset of the ECMA
                     * numeric grammar.  We might not always be so
                     * permissive, so we warn about it.
                     */
                    base = 10
                    c = readDigitsOrThrow(base, c)
                    break
                }
                addToString(c)
                c = getChar()
            }
        }
        if (stringBufferTop == emptyDetector && base != 10) {
            throw ParsingException("number format error")
        }

        if (c == 'n'.code) {
            c = getChar()
        } else if (base == 10 && (c == '.'.code || c == 'e'.code || c == 'E'.code)) {
            if (c == '.'.code) {
                addToString(c)
                c = readDigitsOrThrow(base, getChar())
            }
            if (c == 'e'.code || c == 'E'.code) {
                addToString(c)
                c = getChar()
                if (c == '+'.code || c == '-'.code) {
                    addToString(c)
                    c = getChar()
                }
                if (!isDigit(c)) {
                    throw ParsingException("missing exponent")
                }
                c = readDigitsOrThrow(base, c)
            }
        }
        ungetChar(c)
        tokenEnd = cursor
        return Token.NUMBER
    }
    @Throws(ParsingException::class)
    private fun readDigitsOrThrow(base: Int, firstC: Int): Int {
        val c = readDigits(base, firstC)
        if (c == REPORT_NUMBER_FORMAT_ERROR) {
            throw ParsingException("number format error")
        }
        return c
    }

    @Throws(ParsingException::class)
    private fun readString(quoteChar: Int): Token {
        // We attempt to accumulate a string the fast way, by
        // building it directly out of the reader.  But if there
        // are any escaped characters in the string, we revert to
        // building it out of a StringBuffer.

        // quoteChar: delimiter for last string literal scanned
        stringBufferTop = 0

        var c = getChar(skipFormattingChars = false, ignoreLineEnd = true)
        strLoop@ while (c != quoteChar) {
            var unterminated = false
            if (c == EOF_CHAR) {
                unterminated = true
            } else if (c == NEWLINE) {
                when (lineEndChar) {
                    NEWLINE, CARRIAGE_RETURN -> unterminated = true
                    // Line/Paragraph separators need to be included as is
                    LINE_SEPARATOR, PARAGRAPH_SEPARATOR -> c = lineEndChar
                    else -> Unit
                }
            }

            if (unterminated) {
                throw ParsingException("unterminated string literal")
            }

            if (c == BACKSLASH) {
                // We've hit an escaped character
                c = getChar()
                when (c) {
                    'b'.code -> c = '\b'.code
                    'f'.code -> c = FORM_FEED
                    'n'.code -> c = NEWLINE
                    'r'.code -> c = CARRIAGE_RETURN
                    't'.code -> c = '\t'.code

                    // \v a late addition to the ECMA spec,
                    // it is not in Java, so use 0xb
                    'v'.code -> c = VERTICAL_TAB

                    'u'.code -> {
                        // Get 4 hex digits; if the u escape is not
                        // followed by 4 hex digits, use 'u' + the
                        // literal character sequence that follows.
                        val escapeStart = stringBufferTop
                        addToString('u'.code)
                        var escapeVal = 0
                        var i = 0
                        while (i != UNICODE_ESCAPE_DIGITS) {
                            c = getChar()
                            escapeVal = Kit.xDigitToInt(c, escapeVal)
                            if (escapeVal < 0) {
                                continue@strLoop
                            }
                            addToString(c)
                            i++
                        }
                        // prepare for replace of stored 'u' sequence
                        // by escape value
                        stringBufferTop = escapeStart
                        c = escapeVal
                    }

                    'x'.code -> {
                        // Get 2 hex digits, defaulting to 'x'+literal
                        // sequence, as above.
                        c = getChar()
                        var escapeVal = Kit.xDigitToInt(c, 0)
                        if (escapeVal < 0) {
                            addToString('x'.code)
                            continue@strLoop
                        }
                        val c1 = c
                        c = getChar()
                        escapeVal = Kit.xDigitToInt(c, escapeVal)
                        if (escapeVal < 0) {
                            addToString('x'.code)
                            addToString(c1)
                            continue@strLoop
                        }
                        // got 2 hex digits
                        c = escapeVal
                    }

                    NEWLINE -> {
                        // Remove line terminator after escape to follow
                        // SpiderMonkey and C/C++
                        c = getChar()
                        continue@strLoop
                    }

                    else -> if (isOctalDigit(c)) {
                        c = readOctalEscape(c)
                    }
                }
            }
            addToString(c)
            c = getChar(skipFormattingChars = false)
        }

        tokenEnd = cursor
        return if (quoteChar == '`'.code) Token.TEMPLATE_LITERAL else Token.STRING
    }

    /** Reads an octal escape of up to three digits, the first one being [first]. */
    private fun readOctalEscape(first: Int): Int {
        var value = first - '0'.code
        var c = getChar()
        if (isOctalDigit(c)) {
            value = 8 * value + c - '0'.code
            c = getChar()
            if (isOctalDigit(c) && value <= MAX_OCTAL_ESCAPE_PREFIX) {
                // c is 3rd char of octal sequence only
                // if the resulting val <= 0377
                value = 8 * value + c - '0'.code
                c = getChar()
            }
        }
        ungetChar(c)
        return value
    }

    @Throws(ParsingException::class)
    private fun readPunctuator(c: Int): Token = when (c) {
        ';'.code -> Token.SEMI
        '['.code -> Token.LB
        ']'.code -> Token.RB
        '{'.code -> Token.LC
        '}'.code -> Token.RC
        '('.code -> Token.LP
        ')'.code -> Token.RP
        ','.code -> Token.COMMA
        '?'.code -> Token.HOOK
        ':'.code -> Token.COLON
        '.'.code -> Token.DOT
        '|'.code -> when {
            matchChar('|') -> Token.OR
            matchChar('=') -> Token.ASSIGN_BITOR
            else -> Token.BITOR
        }
        '^'.code -> if (matchChar('=')) Token.ASSIGN_BITXOR else Token.BITXOR
        '&'.code -> when {
            matchChar('&') -> Token.AND
            matchChar('=') -> Token.ASSIGN_BITAND
            else -> Token.BITAND
        }
        '='.code -> when {
            matchChar('=') -> if (matchChar('=')) Token.SHEQ else Token.EQ
            matchChar('>') -> Token.ARROW
            else -> Token.ASSIGN
        }
        '!'.code -> when {
            matchChar('=') -> if (matchChar('=')) Token.SHNE else Token.NE
            else -> Token.NOT
        }
        '<'.code -> readLessThan()
        '>'.code -> readGreaterThan()
        '*'.code -> when {
            matchChar('*') -> if (matchChar('=')) Token.ASSIGN_EXP else Token.EXP
            matchChar('=') -> Token.ASSIGN_MUL
            else -> Token.MUL
        }
        '/'.code -> readSlash()
        '%'.code -> if (matchChar('=')) Token.ASSIGN_MOD else Token.MOD
        '~'.code -> Token.BITNOT
        '+'.code -> when {
            matchChar('=') -> Token.ASSIGN_ADD
            matchChar('+') -> Token.INC
            else -> Token.ADD
        }
        '-'.code -> readMinus()
        // the character itself is not quoted: the code comes from the network
        else -> throw ParsingException("illegal character in JavaScript code")
    }

    private fun readLessThan(): Token {
        /* NB:treat HTML begin-comment as comment-till-eol */
        if (matchChar('!')) {
            if (matchChar('-')) {
                if (matchChar('-')) {
                    tokenBeg = cursor - HTML_COMMENT_START_LENGTH
                    skipLine()
                    return Token.COMMENT
                }
                ungetCharIgnoreLineEnd('-'.code)
            }
            ungetCharIgnoreLineEnd('!'.code)
        }
        if (matchChar('<')) {
            return if (matchChar('=')) Token.ASSIGN_LSH else Token.LSH
        }
        return if (matchChar('=')) Token.LE else Token.LT
    }

    private fun readGreaterThan(): Token {
        if (matchChar('>')) {
            if (matchChar('>')) {
                return if (matchChar('=')) Token.ASSIGN_URSH else Token.URSH
            }
            return if (matchChar('=')) Token.ASSIGN_RSH else Token.RSH
        }
        return if (matchChar('=')) Token.GE else Token.GT
    }

    @Throws(ParsingException::class)
    private fun readSlash(): Token {
        // is it a // comment?
        if (matchChar('/')) {
            tokenBeg = cursor - 2
            skipLine()
            return Token.COMMENT
        }
        // is it a /* or /** comment?
        if (matchChar('*')) {
            return readBlockComment()
        }
        return if (matchChar('=')) Token.ASSIGN_DIV else Token.DIV
    }

    @Throws(ParsingException::class)
    private fun readBlockComment(): Token {
        var lookForSlash = false
        tokenBeg = cursor - 2
        if (matchChar('*')) {
            lookForSlash = true
        }
        while (true) {
            val c = getChar()
            if (c == EOF_CHAR) {
                tokenEnd = cursor - 1
                throw ParsingException("unterminated comment")
            } else if (c == '*'.code) {
                lookForSlash = true
            } else if (c == '/'.code) {
                if (lookForSlash) {
                    tokenEnd = cursor
                    return Token.COMMENT
                }
            } else {
                lookForSlash = false
                tokenEnd = cursor
            }
        }
    }

    private fun readMinus(): Token {
        var t = Token.SUB
        if (matchChar('=')) {
            t = Token.ASSIGN_SUB
        } else if (matchChar('-')) {
            if (!dirtyLine) {
                // treat HTML end-comment after possible whitespace
                // after line start as comment-until-eol
                if (matchChar('>')) {
                    skipLine()
                    return Token.COMMENT
                }
            }
            t = Token.DEC
        }
        dirtyLine = true
        return t
    }
    /*
     * Helper to read the next digits according to the base
     * and ignore the number separator if there is one.
     */
    private fun readDigits(base: Int, firstC: Int): Int {
        if (!isDigit(base, firstC)) {
            return firstC
        }
        addToString(firstC)

        var c = getChar()
        if (c == EOF_CHAR) {
            return EOF_CHAR
        }

        while (true) {
            if (c == NUMERIC_SEPARATOR) {
                // we do no peek here, we are optimistic for performance
                // reasons and because peekChar() only does an getChar/ungetChar.
                c = getChar()
                // if the line ends after the separator we have
                // to report this as an error
                if (c == NEWLINE || c == EOF_CHAR) {
                    return REPORT_NUMBER_FORMAT_ERROR
                }

                if (!isDigit(base, c)) {
                    // bad luck we have to roll back
                    ungetChar(c)
                    return NUMERIC_SEPARATOR
                }
                addToString(NUMERIC_SEPARATOR)
            } else if (isDigit(base, c)) {
                addToString(c)
                c = getChar()
                if (c == EOF_CHAR) {
                    return EOF_CHAR
                }
            } else {
                return c
            }
        }
    }

    /** Parser calls the method when it gets / or /= in literal context. */
    @Throws(ParsingException::class)
    fun readRegExp(startToken: Token) {
        val start = tokenBeg
        stringBufferTop = 0
        if (startToken == Token.ASSIGN_DIV) {
            // Miss-scanned /=
            addToString('='.code)
        } else {
            check(startToken == Token.DIV) { "FAILED ASSERTION" }
            if (peekChar() == '*'.code) {
                tokenEnd = cursor - 1
                throw ParsingException("msg.unterminated.re.lit")
            }
        }

        var inCharSet = false // true if inside a '['..']' pair
        var c = getChar()
        while (c != '/'.code || inCharSet) {
            if (c == NEWLINE || c == EOF_CHAR) {
                throw ParsingException("msg.unterminated.re.lit")
            }
            if (c == BACKSLASH) {
                addToString(c)
                c = getChar()
                if (c == NEWLINE || c == EOF_CHAR) {
                    throw ParsingException("msg.unterminated.re.lit")
                }
            } else if (c == '['.code) {
                inCharSet = true
            } else if (c == ']'.code) {
                inCharSet = false
            }
            addToString(c)
            c = getChar()
        }

        while (true) {
            c = getChar(ignoreLineEnd = true)
            if (c >= 0 && REGEXP_FLAGS.indexOf(c.toChar()) != -1) {
                addToString(c)
            } else if (isAlpha(c)) {
                throw ParsingException("msg.invalid.re.flag")
            } else {
                ungetCharIgnoreLineEnd(c)
                break
            }
        }

        tokenEnd = start + stringBufferTop + 2 // include slashes
    }

    private fun getStringFromBuffer(): String {
        tokenEnd = cursor
        return String(stringBuffer, 0, stringBufferTop)
    }

    private fun addToString(c: Int) {
        val n = stringBufferTop
        if (n == stringBuffer.size) {
            stringBuffer = stringBuffer.copyOf(stringBuffer.size * 2)
        }
        stringBuffer[n] = c.toChar()
        stringBufferTop = n + 1
    }

    private fun ungetChar(c: Int) {
        // can not unread past across line boundary
        check(ungetCursor == 0 || ungetBuffer[ungetCursor - 1] != NEWLINE) { "FAILED ASSERTION" }
        ungetBuffer[ungetCursor++] = c
        cursor--
    }

    private fun matchChar(test: Char): Boolean {
        val c = getChar(ignoreLineEnd = true)
        if (c == test.code) {
            tokenEnd = cursor
            return true
        }
        ungetCharIgnoreLineEnd(c)
        return false
    }

    private fun peekChar(): Int {
        val c = getChar()
        ungetChar(c)
        return c
    }

    private fun getChar(skipFormattingChars: Boolean = true, ignoreLineEnd: Boolean = false): Int {
        if (ungetCursor != 0) {
            cursor++
            return ungetBuffer[--ungetCursor]
        }

        while (true) {
            if (sourceCursor == sourceString.length) {
                return EOF_CHAR
            }
            cursor++
            var c = sourceString[sourceCursor++].code

            if (!ignoreLineEnd && lineEndChar >= 0) {
                if (lineEndChar == CARRIAGE_RETURN && c == NEWLINE) {
                    lineEndChar = NEWLINE
                    continue
                }
                lineEndChar = -1
                lineno++
            }

            if (c <= ASCII_MAX) {
                if (c == NEWLINE || c == CARRIAGE_RETURN) {
                    lineEndChar = c
                    c = NEWLINE
                }
            } else {
                if (c == BYTE_ORDER_MARK) {
                    return c // BOM is considered whitespace
                }
                if (skipFormattingChars && isJSFormatChar(c)) {
                    continue
                }
                if (ScriptRuntime.isJSLineTerminator(c)) {
                    lineEndChar = c
                    c = NEWLINE
                }
            }
            return c
        }
    }

    private fun ungetCharIgnoreLineEnd(c: Int) {
        ungetBuffer[ungetCursor++] = c
        cursor--
    }

    private fun skipLine() {
        // skip to end of line
        var c: Int
        do {
            c = getChar()
        } while (c != EOF_CHAR && c != NEWLINE)
        ungetChar(c)
        tokenEnd = cursor
    }

    private companion object {
        /*
         * For chars - because we need something out-of-range
         * to check.  (And checking EOF by exception is annoying.)
         * Note distinction from EOF token type!
         */
        const val EOF_CHAR = -1

        /*
         * Return value for readDigits() to signal the caller has
         * to return an number format problem.
         */
        const val REPORT_NUMBER_FORMAT_ERROR = -2

        const val BYTE_ORDER_MARK = 0xFEFF
        const val NUMERIC_SEPARATOR = 0x5F // '_'
        const val NEWLINE = 0x0A
        const val CARRIAGE_RETURN = 0x0D
        const val FORM_FEED = 0x0C
        const val VERTICAL_TAB = 0x0B
        const val BACKSLASH = 0x5C
        const val LINE_SEPARATOR = 0x2028 // <LS>
        const val PARAGRAPH_SEPARATOR = 0x2029 // <PS>
        const val ASCII_MAX = 127
        const val NO_BREAK_SPACE = 0xA0

        const val INITIAL_BUFFER_SIZE = 128
        const val UNGET_BUFFER_SIZE = 3
        const val UNICODE_ESCAPE_DIGITS = 4
        const val HTML_COMMENT_START_LENGTH = 4 // "<!--"

        /** 037 in octal: a third octal digit is only read while the value stays <= 0377. */
        const val MAX_OCTAL_ESCAPE_PREFIX = 31

        const val REGEXP_FLAGS = "gimysu"

        /** ECMAScript 6. */
        fun stringToKeywordForES(name: String, isStrict: Boolean): Token = when (name) {
            "break" -> Token.BREAK
            "case" -> Token.CASE
            "catch" -> Token.CATCH
            "const" -> Token.CONST
            "continue" -> Token.CONTINUE
            "debugger" -> Token.DEBUGGER
            "default" -> Token.DEFAULT
            "delete" -> Token.DELPROP
            "do" -> Token.DO
            "else" -> Token.ELSE
            "export" -> Token.EXPORT
            "finally" -> Token.FINALLY
            "for" -> Token.FOR
            "function" -> Token.FUNCTION
            "if" -> Token.IF
            "import" -> Token.IMPORT
            "in" -> Token.IN
            "instanceof" -> Token.INSTANCEOF
            "new" -> Token.NEW
            "return" -> Token.RETURN
            "switch" -> Token.SWITCH
            "this" -> Token.THIS
            "throw" -> Token.THROW
            "try" -> Token.TRY
            "typeof" -> Token.TYPEOF
            "var" -> Token.VAR
            "void" -> Token.VOID
            "while" -> Token.WHILE
            "with" -> Token.WITH
            "yield" -> Token.YIELD
            "false" -> Token.FALSE
            "null" -> Token.NULL
            "true" -> Token.TRUE
            "let" -> Token.LET
            "class", "extends", "super", "await", "enum" -> Token.RESERVED
            "implements", "interface", "package", "private", "protected", "public", "static" ->
                if (isStrict) Token.RESERVED else Token.EOF
            else -> Token.EOF
        }

        fun isAlpha(c: Int): Boolean {
            // Use 'Z' < 'a'
            if (c <= 'Z'.code) {
                return 'A'.code <= c
            }
            return 'a'.code <= c && c <= 'z'.code
        }

        fun isDigit(base: Int, c: Int): Boolean =
            (base == 10 && isDigit(c)) ||
                (base == 16 && isHexDigit(c)) ||
                (base == 8 && isOctalDigit(c)) ||
                (base == 2 && isDualDigit(c))

        fun isDualDigit(c: Int): Boolean = '0'.code == c || c == '1'.code

        fun isOctalDigit(c: Int): Boolean = '0'.code <= c && c <= '7'.code

        fun isDigit(c: Int): Boolean = '0'.code <= c && c <= '9'.code

        fun isHexDigit(c: Int): Boolean =
            ('0'.code <= c && c <= '9'.code) ||
                ('a'.code <= c && c <= 'f'.code) ||
                ('A'.code <= c && c <= 'F'.code)

        /* As defined in ECMA.  jsscan.c uses C isspace() (which allows
         * \v, I think.)  note that code in getChar() implicitly accepts
         * '\r' == \u000D as well.
         */
        fun isJSSpace(c: Int): Boolean {
            if (c <= ASCII_MAX) {
                return c == 0x20 || c == 0x9 || c == 0xC || c == 0xB
            }
            return c == NO_BREAK_SPACE ||
                c == BYTE_ORDER_MARK ||
                Character.getType(c.toChar()) == Character.SPACE_SEPARATOR.toInt()
        }

        fun isJSFormatChar(c: Int): Boolean =
            c > ASCII_MAX && Character.getType(c.toChar()) == Character.FORMAT.toInt()
    }
}
