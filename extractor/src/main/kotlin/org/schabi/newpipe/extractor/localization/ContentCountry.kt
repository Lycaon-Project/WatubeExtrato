package org.schabi.newpipe.extractor.localization

import java.io.IOException
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.Collections

/**
 * Represents a country that should be used when fetching content.
 *
 * YouTube, for example, give different results in their feed depending on which country is
 * selected.
 *
 * The code ends up in URLs and request bodies, so it is checked like the country of a
 * [Localization]: ASCII letters or digits only, eight at most.
 *
 * @throws IllegalArgumentException if [countryCode] is not of that form
 */
public open class ContentCountry(countryCode: String) : Serializable {

    public val countryCode: String = LocaleCodes.requireRegionCode(countryCode)

    override fun toString(): String = countryCode

    /** Deserialisation does not run the constructor: the code is checked again. */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        try {
            LocaleCodes.requireRegionCode(countryCode)
        } catch (_: RuntimeException) {
            // a malformed code, or none at all
            throw InvalidObjectException("Invalid country code")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ContentCountry) {
            return false
        }
        return countryCode == other.countryCode
    }

    override fun hashCode(): Int = countryCode.hashCode()

    public companion object {
        private const val serialVersionUID: Long = 1L

        @JvmField
        public val DEFAULT: ContentCountry = ContentCountry(Localization.DEFAULT.countryCode)

        @JvmStatic
        public fun listFrom(vararg countryCodeList: String): List<ContentCountry> {
            val toReturn = ArrayList<ContentCountry>(countryCodeList.size)
            for (countryCode in countryCodeList) {
                toReturn.add(ContentCountry(countryCode))
            }
            return Collections.unmodifiableList(toReturn)
        }
    }
}
