package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.JavaScript
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.TimeLimitedCharSequence
import org.schabi.newpipe.extractor.utils.jsextractor.JavaScriptExtractor
import java.util.regex.Pattern

/**
 * Utility class to deobfuscate the signature of streaming URLs from HTML5 clients with YouTube's
 * base JavaScript player.
 */
internal object YoutubeSignatureUtils {

    /**
     * The name of the deobfuscation function which needs to be called inside the deobfuscation
     * code.
     */
    const val DEOBFUSCATION_FUNCTION_NAME: String = "deobfuscate"

    // Longer than the usual line limit on purpose: these patterns are easier to compare with
    // upstream when kept on one line each
    private val FUNCTION_REGEXES: Array<Pattern> = arrayOf(
        Pattern.compile("(\\w*)=function(.*)\\{.*set\\(\\\"alr\\\",\\\"yes\\\"\\);\\w&&"),
        Pattern.compile("\\b(?:[a-zA-Z0-9_$]+)&&\\((?:[a-zA-Z0-9_$]+)=([a-zA-Z0-9_$]{2,})\\((\\d+,)decodeURIComponent\\((?:[a-zA-Z0-9_$]+)\\)\\)"),
        Pattern.compile("\\b(?:[a-zA-Z0-9_$]+)&&\\((?:[a-zA-Z0-9_$]+)=([a-zA-Z0-9_$]{2,})\\(decodeURIComponent\\((?:[a-zA-Z0-9_$]+)\\)\\)"),
        Pattern.compile("\\bm=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(h\\.s\\)\\)"),
        Pattern.compile("\\bc&&\\(c=([a-zA-Z0-9$]{2,})\\(decodeURIComponent\\(c\\)\\)"),
        Pattern.compile("(?:\\b|[^a-zA-Z0-9$])([a-zA-Z0-9$]{2,})\\s*=\\s*function\\(\\s*a\\s*\\)\\s*\\{\\s*a\\s*=\\s*a\\.split\\(\\s*\"\"\\s*\\)"),
        Pattern.compile("([\\w$]+)\\s*=\\s*function\\((\\w+)\\)\\{\\s*\\2=\\s*\\2\\.split\\(\"\"\\)\\s*;"),
    )

    /**
     * A JavaScript identifier as the patterns above capture it: the function name read from the
     * player is placed in the generated code, so it must not be able to carry anything else.
     */
    private val JS_IDENTIFIER = Regex("[a-zA-Z0-9_$]{1,64}")

    /** The extra leading argument some patterns capture (`123,`): placed in the generated code too. */
    private val ADDITIONAL_PARAMS = Regex("(\\d{1,9},)?")

    private const val DEOBF_FUNC_REGEX_START = "("
    private const val DEOBF_FUNC_REGEX_END = "=function\\([a-zA-Z0-9_]+\\)\\{.+?\\})"

    private val SIG_DEOBF_GLOBAL_ARRAY_REGEX: Pattern =
        Pattern.compile("(var [A-z]+=['\"].*['\"].split\\(\"[;{]\"\\))")
    private val SIG_DEOBF_HELPER_OBJ_NAME_REGEX: Pattern =
        Pattern.compile("[;,]([A-Za-z0-9_$]{2,})\\[..")
    private const val SIG_DEOBF_HELPER_OBJ_REGEX_START = "(var "

    // any character, \r included, which (?>.|\n) missed; and a plain character class, which
    // every regex engine repeats without growing its stack, unlike a group
    private const val SIG_DEOBF_HELPER_OBJ_REGEX_END = "=\\{[\\s\\S]+?\\}\\};)"

    /**
     * Get the signature deobfuscation code of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the signature deobfuscation code
     * @throws ParsingException if the signature deobfuscation code couldn't be extracted
     */
    @JvmStatic
    @Throws(ParsingException::class)
    fun getDeobfuscationCode(javaScriptPlayerCode: String): String {
        // every regex below reads the player through this view, so none can run for minutes
        val boundedPlayerCode: CharSequence = TimeLimitedCharSequence(
            javaScriptPlayerCode, YoutubeJavaScriptPlayerManager.PLAYER_CODE_REGEX_BUDGET,
        )
        try {
            val (deobfuscationFunctionName, functionAdditionalParams) =
                getDeobfuscationFunctionNameAndParams(boundedPlayerCode)

            val deobfuscationFunction = try {
                getDeobfuscateFunctionWithLexer(javaScriptPlayerCode, deobfuscationFunctionName)
            } catch (_: Exception) {
                getDeobfuscateFunctionWithRegex(boundedPlayerCode, deobfuscationFunctionName)
            } catch (_: StackOverflowError) {
                getDeobfuscateFunctionWithRegex(boundedPlayerCode, deobfuscationFunctionName)
            }

            // Assert the extracted deobfuscation function is valid
            JavaScript.compileOrThrow(deobfuscationFunction)

            val globalVar = Parser.matchGroup1(SIG_DEOBF_GLOBAL_ARRAY_REGEX, boundedPlayerCode)

            val helperObjectName =
                Parser.matchGroup1(SIG_DEOBF_HELPER_OBJ_NAME_REGEX, deobfuscationFunction)

            val helperObject = getHelperObject(boundedPlayerCode, helperObjectName)

            val callerFunction = "function $DEOBFUSCATION_FUNCTION_NAME(a){return " +
                "$deobfuscationFunctionName($functionAdditionalParams" + "a);}"

            return "$globalVar;$helperObject$deobfuscationFunction;$callerFunction"
        } catch (e: Exception) {
            throw ParsingException("Could not parse deobfuscation function", e)
        } catch (e: StackOverflowError) {
            throw ParsingException("Could not parse deobfuscation function", e)
        }
    }

    @Throws(ParsingException::class)
    private fun getDeobfuscationFunctionNameAndParams(
        javaScriptPlayerCode: CharSequence,
    ): kotlin.Pair<String, String> {
        val matcher = try {
            Parser.matchMultiplePatterns(FUNCTION_REGEXES, javaScriptPlayerCode)
        } catch (e: Parser.RegexException) {
            throw ParsingException(
                "Could not find deobfuscation function with any of the known patterns", e,
            )
        }
        val functionName = matcher.group(1)
        if (functionName == null || !JS_IDENTIFIER.matches(functionName)) {
            throw ParsingException("Invalid deobfuscation function name")
        }
        val functionAdditionalParams = if (matcher.groupCount() > 1) {
            matcher.group(2).orEmpty()
        } else {
            ""
        }
        if (!ADDITIONAL_PARAMS.matches(functionAdditionalParams)) {
            throw ParsingException("Invalid deobfuscation function parameters")
        }
        return functionName to functionAdditionalParams
    }

    @Throws(ParsingException::class)
    private fun getDeobfuscateFunctionWithLexer(
        javaScriptPlayerCode: String,
        deobfuscationFunctionName: String,
    ): String {
        val functionBase = "$deobfuscationFunctionName=function"
        return functionBase +
            JavaScriptExtractor.matchToClosingBrace(javaScriptPlayerCode, functionBase)
    }

    @Throws(ParsingException::class)
    private fun getDeobfuscateFunctionWithRegex(
        javaScriptPlayerCode: CharSequence,
        deobfuscationFunctionName: String,
    ): String {
        val functionPattern = DEOBF_FUNC_REGEX_START +
            Pattern.quote(deobfuscationFunctionName) +
            DEOBF_FUNC_REGEX_END
        return "var " + Parser.matchGroup1(functionPattern, javaScriptPlayerCode)
    }

    @JvmStatic
    @Throws(ParsingException::class)
    fun getHelperObject(javaScriptPlayerCode: CharSequence, helperObjectName: String): String {
        val helperPattern = SIG_DEOBF_HELPER_OBJ_REGEX_START +
            Pattern.quote(helperObjectName) +
            SIG_DEOBF_HELPER_OBJ_REGEX_END
        return Parser.matchGroup1(helperPattern, javaScriptPlayerCode).replace("\n", "")
    }
}
