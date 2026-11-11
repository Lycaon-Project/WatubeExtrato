package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.JavaScript
import org.schabi.newpipe.extractor.utils.Parser
import org.schabi.newpipe.extractor.utils.TimeLimitedCharSequence
import org.schabi.newpipe.extractor.utils.jsextractor.JavaScriptExtractor
import java.util.Locale
import java.util.regex.Pattern

/**
 * Utility class to get the throttling parameter decryption code and check if a streaming has the
 * throttling parameter.
 */
internal object YoutubeThrottlingParameterUtils {

    /**
     * The name of the deobfuscation function which needs to be called inside the deobfuscation
     * code.
     */
    const val DEOBFUSCATION_FUNCTION_NAME: String = "deobfuscate_n_param"

    // NOTE: When changing this, you should also change the quick exit/shortcut
    // in getThrottlingParameterFromStreamingUrl
    private val THROTTLING_PARAM_PATTERN: Pattern = Pattern.compile("[&?]n=([^&]+)")

    /** What a deobfuscated throttling parameter is made of: it is put in a URL as it is. */
    private val URL_SAFE_PARAMETER: Pattern = Pattern.compile("[A-Za-z0-9_-]+")

    private val DEOBFUSCATION_FUNCTION_NAME_REGEXES: Array<Pattern> = arrayOf(
        /*
         * Matches the following text, where we want QO:
         *
         * QO=function(... F.set("alr","yes" ...)
         */
        Pattern.compile("(\\w*)=function\\(.*\\)\\{.*set\\(\\\"alr\\\",\\\"yes\\\"\\);\\w&&"),
    )

    /**
     * A JavaScript identifier as the patterns above capture it: the function name read from the
     * player is placed in the generated code, so it must not be able to carry anything else.
     */
    private val JS_IDENTIFIER = Regex("[a-zA-Z0-9_$]{1,64}")

    // Escape the curly end brace to allow compatibility with Android's regex engine
    // See https://stackoverflow.com/q/45074813
    private const val DEOBFUSCATION_FUNCTION_BODY_REGEX =
        "=\\s*function([\\S\\s]*?\\}\\s*return [\\w$]+?\\.join\\(\"\"\\)\\s*\\};)"

    private const val DEOBFUSCATION_FUNCTION_ARRAY_OBJECT_TYPE_DECLARATION_REGEX = "var "

    private const val FUNCTION_NAMES_IN_DEOBFUSCATION_ARRAY_REGEX = "\\s*=\\s*\\[(.+?)][;,]"

    /** Stubs of the browser globals the player code expects, put before it. */
    private val HELPER_FUNCTION_PREFIX = """
        var g = {};
        if (typeof globalThis.XMLHttpRequest === "undefined") {
            globalThis.XMLHttpRequest = { prototype: {} };
        }
        if (typeof URL === "undefined") {
            globalThis.location = {
                hash: "",
                host: "www.youtube.com",
                hostname: "www.youtube.com",
                href: "https://www.youtube.com/",
                origin: "https://www.youtube.com",
                password: "",
                pathname: "/",
                port: "",
                protocol: "https:",
                search: "",
                username: "",
            };
        } else {
            globalThis.location = new URL("https://www.youtube.com");
        }
        if (typeof globalThis.document === "undefined") {
            globalThis.document = Object.create(null);
        }
        if (typeof globalThis.navigator === "undefined") {
            globalThis.navigator = Object.create(null);
        }
        if (typeof globalThis.self === "undefined") {
            globalThis.self = globalThis;
        }
        if (typeof globalThis.window === "undefined") {
            globalThis.window = globalThis;
        }
    """.trimIndent() + "\n"

    /** The caller function, put after the player code; formatted with two `%s`. */
    private val HELPER_FUNCTION_SUFFIX_FORMAT = """
        function %s(n){
          const url = %s("https://www.youtube.com/", "s", undefined);
          url.set("n", n);
          const proto = Object.getPrototypeOf(url);
          const keys = Object.keys(proto).concat(Object.getOwnPropertyNames(proto));
          for (let i = 0; i < keys.length; i++) {
            const key = keys[i];
            if (!["constructor", "set", "get", "clone"].includes(key)) {
              url[key]();
              break;
            }
          }
          return url.get("n");
        }
    """.trimIndent()

    /**
     * Get the throttling parameter deobfuscation function name of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the name of the throttling parameter deobfuscation function
     * @throws ParsingException if the name of the throttling parameter deobfuscation function
     * could not be extracted
     */
    @JvmStatic
    @Throws(ParsingException::class)
    fun getDeobfuscationFunctionName(javaScriptPlayerCode: CharSequence): String {
        val matcher = try {
            Parser.matchMultiplePatterns(DEOBFUSCATION_FUNCTION_NAME_REGEXES, javaScriptPlayerCode)
        } catch (e: Parser.RegexException) {
            throw ParsingException(
                "Could not find deobfuscation function with any of the " +
                    "known patterns in the base JavaScript player code",
                e,
            )
        }

        val functionName = requireIdentifier(matcher.group(1))
        if (matcher.groupCount() == 1) {
            return functionName
        }

        val arrayNum = matcher.group(2)?.toIntOrNull()
            ?: throw ParsingException("Invalid deobfuscation function index")
        val arrayPattern = Pattern.compile(
            DEOBFUSCATION_FUNCTION_ARRAY_OBJECT_TYPE_DECLARATION_REGEX +
                Pattern.quote(functionName) +
                FUNCTION_NAMES_IN_DEOBFUSCATION_ARRAY_REGEX,
        )
        val arrayStr = Parser.matchGroup1(arrayPattern, javaScriptPlayerCode)
        val names = arrayStr.split(',')
        return requireIdentifier(names.getOrNull(arrayNum))
    }

    /**
     * Get the throttling parameter deobfuscation code of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the throttling parameter deobfuscation function code
     * @throws ParsingException if the throttling parameter deobfuscation code couldn't be
     * extracted
     */
    @JvmStatic
    @Throws(ParsingException::class)
    fun getDeobfuscationFunction(javaScriptPlayerCode: String, functionName: String): String =
        getDeobfuscationFunction(javaScriptPlayerCode, javaScriptPlayerCode, functionName)

    @Throws(ParsingException::class)
    private fun getDeobfuscationFunction(
        javaScriptPlayerCode: String,
        boundedPlayerCode: CharSequence,
        functionName: String,
    ): String = try {
        parseFunctionWithLexer(javaScriptPlayerCode, functionName)
    } catch (_: Exception) {
        parseFunctionWithRegex(boundedPlayerCode, functionName)
    } catch (_: StackOverflowError) {
        parseFunctionWithRegex(boundedPlayerCode, functionName)
    }

    /**
     * Get the throttling parameter deobfuscation code of YouTube's base JavaScript file.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @return the throttling parameter deobfuscation function code
     * @throws ParsingException if the throttling parameter deobfuscation code couldn't be
     * extracted
     */
    @JvmStatic
    @Throws(ParsingException::class)
    fun getDeobfuscationCode(javaScriptPlayerCode: String): String {
        // every regex below reads the player through this view, so none can run for minutes
        val boundedPlayerCode: CharSequence = TimeLimitedCharSequence(
            javaScriptPlayerCode, YoutubeJavaScriptPlayerManager.PLAYER_CODE_REGEX_BUDGET,
        )
        try {
            val functionName = getDeobfuscationFunctionName(boundedPlayerCode)
            val deobfuscationFunction =
                getDeobfuscationFunction(javaScriptPlayerCode, boundedPlayerCode, functionName)

            // Assert the extracted deobfuscation function is valid
            JavaScript.compileOrThrow(deobfuscationFunction)

            return buildHelperFunction(javaScriptPlayerCode, deobfuscationFunction)
        } catch (e: RuntimeException) {
            throw ParsingException("Could not parse throttling parameter deobfuscation function", e)
        } catch (e: StackOverflowError) {
            throw ParsingException("Could not parse throttling parameter deobfuscation function", e)
        }
    }

    /**
     * @param parameter a deobfuscated throttling parameter
     * @return whether it can be put in a streaming URL as it is, without changing anything else
     * in it
     */
    @JvmStatic
    fun isValidThrottlingParameter(parameter: String): Boolean =
        URL_SAFE_PARAMETER.matcher(parameter).matches()

    /**
     * Replace the value of the throttling parameter of a streaming URL, and only this one: the
     * same text elsewhere in the URL, in another parameter, is left as it is.
     *
     * @param streamingUrl a streaming URL
     * @param value        the new value of its throttling parameter
     * @return the streaming URL with this value, or the same URL if it has no throttling parameter
     */
    @JvmStatic
    fun replaceThrottlingParameter(streamingUrl: String, value: String): String {
        val matcher = THROTTLING_PARAM_PATTERN.matcher(streamingUrl)
        if (!matcher.find()) {
            return streamingUrl
        }
        return streamingUrl.substring(0, matcher.start(1)) + value +
            streamingUrl.substring(matcher.end(1))
    }

    /**
     * Get the throttling parameter of a streaming URL if it exists.
     *
     * @param streamingUrl a streaming URL
     * @return the throttling parameter of the streaming URL or `null` if no parameter has
     * been found
     */
    @JvmStatic
    fun getThrottlingParameterFromStreamingUrl(streamingUrl: String): String? {
        // Do a quick check if the n parameter is even present, if not abort
        // This improves performance by 60-900x
        if (!streamingUrl.contains("&n=") && !streamingUrl.contains("?n=")) {
            return null
        }
        val matcher = THROTTLING_PARAM_PATTERN.matcher(streamingUrl)
        // If the throttling parameter could not be parsed from the URL, it means that there is
        // no throttling parameter: return null in this case
        return if (matcher.find()) matcher.group(1) else null
    }

    @Throws(ParsingException::class)
    private fun requireIdentifier(name: String?): String =
        name?.takeIf { JS_IDENTIFIER.matches(it) }
            ?: throw ParsingException("Invalid deobfuscation function name")

    @Throws(ParsingException::class)
    private fun parseFunctionWithLexer(javaScriptPlayerCode: String, functionName: String): String {
        val functionBase = "$functionName=function"
        return functionBase +
            JavaScriptExtractor.matchToClosingBrace(javaScriptPlayerCode, functionBase) + ";"
    }

    @Throws(Parser.RegexException::class)
    private fun parseFunctionWithRegex(
        javaScriptPlayerCode: CharSequence,
        functionName: String,
    ): String {
        // Quote the function name, as it may contain special regex characters such as dollar
        val functionPattern = Pattern.compile(
            Pattern.quote(functionName) + DEOBFUSCATION_FUNCTION_BODY_REGEX,
            Pattern.DOTALL,
        )
        return validateFunction(
            "function " + functionName + Parser.matchGroup1(functionPattern, javaScriptPlayerCode),
        )
    }

    private fun validateFunction(function: String): String {
        JavaScript.compileOrThrow(function)
        return function
    }

    /**
     * Builds a helper function to execute the deobfuscation function.
     *
     * @param javaScriptPlayerCode the complete JavaScript base player code
     * @param deobfuscationFunctionName the name of the throttling parameter deobfuscation function
     * @return code needed to execute the deobfuscation function
     */
    private fun buildHelperFunction(
        javaScriptPlayerCode: String,
        deobfuscationFunctionName: String,
    ): String {
        val strippedJavaScriptPlayerCode = javaScriptPlayerCode
            .replace("var _yt_player={};(function(g){var window=this;", "")
            .replace("})(_yt_player);", "")
        return HELPER_FUNCTION_PREFIX + strippedJavaScriptPlayerCode + String.format(
            Locale.ROOT,
            HELPER_FUNCTION_SUFFIX_FORMAT,
            DEOBFUSCATION_FUNCTION_NAME,
            deobfuscationFunctionName,
        )
    }
}
