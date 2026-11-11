package org.schabi.newpipe.extractor.localization

import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.utils.LocaleCompat
import java.io.IOException
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.Serializable
import java.util.Collections
import java.util.Locale
import java.util.Optional

/**
 * A language, and optionally a country, used to fetch content.
 *
 * Both codes end up in request headers, URLs and request bodies, so they are checked when
 * created: only ASCII letters (and digits for the country, e.g. `419`) of a sensible length are
 * accepted, which rules out header, URL or JSON injection.
 *
 * @throws IllegalArgumentException if a code is not of that form
 */
public open class Localization @JvmOverloads constructor(
    languageCode: String,
    countryCode: String? = null,
) : Serializable {

    public val languageCode: String = LocaleCodes.requireLanguageCode(languageCode)

    private val nullableCountryCode: String? = countryCode?.let(LocaleCodes::requireRegionCode)

    public val countryCode: String
        get() = nullableCountryCode.orEmpty()

    /**
     * Return a formatted string in the form of: `language-Country`, or
     * just `language` if country is `null`.
     *
     * @return A correctly formatted localizationCode for this localization.
     */
    public val localizationCode: String
        get() = if (nullableCountryCode == null) languageCode else "$languageCode-$nullableCountryCode"

    override fun toString(): String = "Localization[$localizationCode]"

    /**
     * Deserialisation does not run the constructor: the codes are checked again, so that a
     * crafted saved state (an extra of an intent) cannot put a line break in a request header.
     */
    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(input: ObjectInputStream) {
        input.defaultReadObject()
        try {
            LocaleCodes.requireLanguageCode(languageCode)
            nullableCountryCode?.let(LocaleCodes::requireRegionCode)
        } catch (_: RuntimeException) {
            // a malformed code, or none at all
            throw InvalidObjectException("Invalid localization code")
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Localization) {
            return false
        }
        return languageCode == other.languageCode && nullableCountryCode == other.nullableCountryCode
    }

    override fun hashCode(): Int =
        HASH_MULTIPLIER * languageCode.hashCode() + nullableCountryCode.hashCode()

    public companion object {
        private const val serialVersionUID: Long = 1L

        private const val HASH_MULTIPLIER = 31

        @JvmField
        public val DEFAULT: Localization = Localization("en", "GB")

        /**
         * Three letter language codes (ISO 639-2/T) to their [Locale], built once on first use
         * instead of on every call: there are about 190 of them.
         */
        private val THREE_LETTER_LOCALES: Map<String, Locale> by lazy {
            val languages = Locale.getISOLanguages()
            val localeMap = HashMap<String, Locale>(languages.size)
            for (language in languages) {
                val locale = Locale.Builder().setLanguage(language).build()
                localeMap[locale.isO3Language] = locale
            }
            localeMap
        }

        /**
         * @param localizationCodeList a list of localization code, formatted like
         * [localizationCode]
         * @throws IllegalArgumentException If any of the localizationCodeList is formatted
         * incorrectly
         * @return list of Localization objects
         */
        @JvmStatic
        public fun listFrom(vararg localizationCodeList: String): List<Localization> {
            val toReturn = ArrayList<Localization>(localizationCodeList.size)
            for (localizationCode in localizationCodeList) {
                val localization = fromLocalizationCode(localizationCode)
                require(localization.isPresent) { "Not a localization code" }
                toReturn.add(localization.get())
            }
            return Collections.unmodifiableList(toReturn)
        }

        /**
         * @param localizationCode a localization code, formatted like [localizationCode]
         * @return A Localization, if the code was valid.
         */
        @JvmStatic
        public fun fromLocalizationCode(localizationCode: String): Optional<Localization> {
            val locale = LocaleCompat.forLanguageTag(localizationCode)
            if (!locale.isPresent) {
                return Optional.empty()
            }
            return try {
                Optional.of(fromLocale(locale.get()))
            } catch (_: IllegalArgumentException) {
                Optional.empty()
            }
        }

        /** @throws IllegalArgumentException if the language or country of [locale] is unusable */
        @JvmStatic
        public fun fromLocale(locale: Locale): Localization = Localization(locale.language, locale.country)

        /**
         * Converts a three letter language code (ISO 639-2/T) to a Locale
         * because limits of Java Locale class.
         *
         * @param code a three letter language code
         * @return the Locale corresponding
         */
        @JvmStatic
        @Throws(ParsingException::class)
        public fun getLocaleFromThreeLetterCode(code: String): Locale = THREE_LETTER_LOCALES[code]
            ?: throw ParsingException("Could not get Locale from this three letter language code")
    }
}

/** Checks of the language and region codes of [Localization] and [ContentCountry]. */
internal object LocaleCodes {

    /** ISO 639 / BCP 47 primary language subtag. */
    private val LANGUAGE_CODE = Regex("[A-Za-z]{2,8}")

    /**
     * Region: ISO 3166 letters or UN M.49 digits (`419`), possibly empty (from [Locale.getCountry])
     * or longer when a script subtag ended up there (`sr-Latn` read as language and country).
     */
    private val REGION_CODE = Regex("[A-Za-z0-9]{0,8}")

    fun requireLanguageCode(code: String): String {
        require(LANGUAGE_CODE.matches(code)) { "Invalid language code" }
        return code
    }

    fun requireRegionCode(code: String): String {
        require(REGION_CODE.matches(code)) { "Invalid country code" }
        return code
    }
}
