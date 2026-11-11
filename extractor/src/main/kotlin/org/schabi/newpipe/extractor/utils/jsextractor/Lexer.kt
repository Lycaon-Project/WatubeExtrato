package org.schabi.newpipe.extractor.utils.jsextractor

import org.schabi.newpipe.extractor.exceptions.ParsingException
import java.util.ArrayDeque

/**
 * JavaScript lexer that is able to parse JavaScript code and return its
 * tokens.
 *
 * The algorithm for distinguishing between division operators and regex literals
 * was taken from the [RESS lexer](https://github.com/rusty-ecma/RESS/).
 *
 * @param js JavaScript code
 * @param startOffset where the lexer starts reading [js]; token positions are relative to it
 */
public open class Lexer internal constructor(js: String, startOffset: Int) {

    private class Paren(val funcExpr: Boolean, val conditional: Boolean)

    private class Brace(val isBlock: Boolean, val paren: Paren?)

    private open class MetaToken(val token: Token, val lineno: Int)

    private class BraceMetaToken(token: Token, lineno: Int, val brace: Brace) :
        MetaToken(token, lineno)

    private class ParenMetaToken(token: Token, lineno: Int, val paren: Paren) :
        MetaToken(token, lineno)

    private class LookBehind {
        private val list = arrayOfNulls<MetaToken>(3)

        fun push(t: MetaToken) {
            var toShift: MetaToken? = t
            for (i in list.indices) {
                val tmp = list[i]
                list[i] = toShift
                toShift = tmp
            }
        }

        fun one(): MetaToken? = list[0]

        fun two(): MetaToken? = list[1]

        fun three(): MetaToken? = list[2]

        fun oneIs(token: Token): Boolean = list[0]?.token == token

        fun twoIs(token: Token): Boolean = list[1]?.token == token
    }

    /**
     * Parsed token, containing the token and its position in the input string
     */
    public class ParsedToken internal constructor(
        @JvmField public val token: Token,
        @JvmField public val start: Int,
        @JvmField public val end: Int,
    )

    private val stream = EcmaScriptTokenStream(js, startOffset, 0, false)
    private val lastThree = LookBehind()

    // ArrayDeque instead of the synchronized java.util.Stack: one lexer runs on one thread
    private val braceStack = ArrayDeque<Brace>()
    private val parenStack = ArrayDeque<Paren>()

    /**
     * Create a new JavaScript lexer with the given source code
     *
     * @param js JavaScript code
     */
    public constructor(js: String) : this(js, 0)

    /**
     * Continue parsing and return the next token.
     *
     * A function, not a property: every call consumes input.
     *
     * @return next token
     * @throws ParsingException if the code cannot be read
     */
    @Throws(ParsingException::class)
    public fun getNextToken(): ParsedToken {
        var token = stream.nextToken()

        if ((token == Token.DIV || token == Token.ASSIGN_DIV) && isRegexStart()) {
            stream.readRegExp(token)
            token = Token.REGEXP
        }

        val parsedToken = ParsedToken(token, stream.tokenBeg, stream.tokenEnd)
        keepBooks(parsedToken)
        return parsedToken
    }

    /**
     * Whether the parser is balanced (equal amount of open and closed parentheses and braces)
     */
    public val isBalanced: Boolean
        get() = braceStack.isEmpty() && parenStack.isEmpty()

    /**
     * Evaluate the token for possible regex start and handle updating the
     * `self.last_three`, `self.paren_stack` and `self.brace_stack`
     */
    @Throws(ParsingException::class)
    private fun keepBooks(parsedToken: ParsedToken) {
        if (parsedToken.token.isPunct) {
            when (parsedToken.token) {
                Token.LP -> {
                    handleOpenParenBooks()
                    return
                }
                Token.LC -> {
                    handleOpenBraceBooks()
                    return
                }
                Token.RP -> {
                    handleCloseParenBooks(parsedToken.start)
                    return
                }
                Token.RC -> {
                    handleCloseBraceBooks(parsedToken.start)
                    return
                }
                else -> Unit
            }
        }
        if (parsedToken.token != Token.COMMENT) {
            lastThree.push(MetaToken(parsedToken.token, stream.lineno))
        }
    }

    /**
     * Handle the book keeping when we find an `(`
     */
    private fun handleOpenParenBooks() {
        var funcExpr = false
        if (lastThree.oneIs(Token.FUNCTION)) {
            funcExpr = lastThree.two()?.let { checkForExpression(it.token) } == true
        } else if (lastThree.twoIs(Token.FUNCTION)) {
            funcExpr = lastThree.three()?.let { checkForExpression(it.token) } == true
        }

        val conditional = lastThree.one()?.token?.isConditional == true

        val paren = Paren(funcExpr, conditional)
        parenStack.push(paren)
        lastThree.push(ParenMetaToken(Token.LP, stream.lineno, paren))
    }

    /**
     * Handle the book keeping when we find an `{`
     */
    private fun handleOpenBraceBooks() {
        var isBlock = true
        val one = lastThree.one()
        if (one != null) {
            isBlock = when (one.token) {
                Token.LP, Token.LC, Token.CASE -> false
                Token.COLON -> braceStack.peek()?.isBlock == true
                Token.RETURN, Token.YIELD, Token.YIELD_STAR ->
                    lastThree.two()?.let { it.lineno != stream.lineno } == true
                else -> !one.token.isOp
            }
        }

        val paren = if (one is ParenMetaToken && one.token == Token.RP) one.paren else null
        val brace = Brace(isBlock, paren)
        braceStack.push(brace)
        lastThree.push(BraceMetaToken(Token.LC, stream.lineno, brace))
    }

    /**
     * Handle the book keeping when we find an `)`
     */
    @Throws(ParsingException::class)
    private fun handleCloseParenBooks(start: Int) {
        if (parenStack.isEmpty()) {
            throw ParsingException("unmatched closing paren at $start")
        }
        lastThree.push(ParenMetaToken(Token.RP, stream.lineno, parenStack.pop()))
    }

    /**
     * Handle the book keeping when we find an `}`
     */
    @Throws(ParsingException::class)
    private fun handleCloseBraceBooks(start: Int) {
        if (braceStack.isEmpty()) {
            throw ParsingException("unmatched closing brace at $start")
        }
        lastThree.push(BraceMetaToken(Token.RC, stream.lineno, braceStack.pop()))
    }

    private fun checkForExpression(token: Token): Boolean =
        token.isOp || token == Token.RETURN || token == Token.CASE

    /**
     * Detect if the `/` is the beginning of a regex or is division
     * [see this for more details](https://github.com/sweet-js/sweet-core/wiki/design)
     *
     * @return isRegexStart
     */
    private fun isRegexStart(): Boolean {
        val one = lastThree.one() ?: return true
        val t = one.token
        return when {
            t.isKeyw -> t != Token.THIS
            t == Token.RP && one is ParenMetaToken -> one.paren.conditional
            t == Token.RC && one is BraceMetaToken -> {
                val brace = one.brace
                if (brace.isBlock) {
                    brace.paren?.let { !it.funcExpr } ?: true
                } else {
                    false
                }
            }
            t.isPunct -> t != Token.RB
            else -> false
        }
    }
}
