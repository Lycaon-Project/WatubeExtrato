package org.schabi.newpipe.extractor.utils

import org.jsoup.Jsoup
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonParser
import org.schabi.newpipe.extractor.json.JsonParserException

public object JsonUtils {

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getValue(`object`: JsonObject, path: String): Any {
        val keys = path.split('.')
        val parentObject = getObject(`object`, keys.subList(0, keys.size - 1))
        return parentObject[keys[keys.size - 1]]
            ?: throw ParsingException("Unable to get $path")
    }

    @Throws(ParsingException::class)
    private inline fun <reified T : Any> getInstanceOf(`object`: JsonObject, path: String): T =
        getValue(`object`, path) as? T ?: throw ParsingException("Wrong data type at path $path")

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getString(`object`: JsonObject, path: String): String = getInstanceOf(`object`, path)

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getBoolean(`object`: JsonObject, path: String): Boolean =
        getInstanceOf(`object`, path)

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getNumber(`object`: JsonObject, path: String): Number = getInstanceOf(`object`, path)

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getObject(`object`: JsonObject, path: String): JsonObject =
        getInstanceOf(`object`, path)

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getArray(`object`: JsonObject, path: String): JsonArray = getInstanceOf(`object`, path)

    /** A missing key gives an empty object, as [JsonObject.getObject] does. */
    private fun getObject(`object`: JsonObject, keys: List<String>): JsonObject {
        var result = `object`
        for (key in keys) {
            result = result.getObject(key)
        }
        return result
    }

    @JvmStatic
    @Throws(ParsingException::class)
    public fun toJsonObject(responseBody: String): JsonObject =
        try {
            JsonParser.`object`().from(responseBody)
        } catch (e: JsonParserException) {
            // the parser's messages hold positions only, never the parsed text
            throw ParsingException("Could not parse JSON", e)
        }

    /**
     * Get an attribute of a web page as JSON
     *
     * Originally a part of bandcampDirect.
     *
     * Example HTML:
     * ```
     * <p data-town="{&quot;name&quot;:&quot;Mycenae&quot;,&quot;country&quot;:&quot;Greece&quot;}">
     * This is Sparta!</p>
     * ```
     *
     * Calling this function to get the attribute `data-town` returns the JsonObject for
     * ```
     *   {
     *     "name": "Mycenae",
     *     "country": "Greece"
     *   }
     * ```
     *
     * @param html     The HTML where the JSON we're looking for is stored inside a
     * variable inside some JavaScript block
     * @param variable Name of the variable
     * @return The JsonObject stored in the variable with this name
     */
    @JvmStatic
    @Throws(JsonParserException::class, ArrayIndexOutOfBoundsException::class)
    public fun getJsonData(html: String, variable: String): JsonObject {
        val document = Jsoup.parse(html)
        val json = document.getElementsByAttribute(variable).attr(variable)
        return JsonParser.`object`().from(json)
    }

    @JvmStatic
    public fun getStringListFromJsonArray(array: JsonArray): List<String> =
        array.filterIsInstance<String>()
}
