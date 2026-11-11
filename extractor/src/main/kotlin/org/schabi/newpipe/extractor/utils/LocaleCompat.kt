package org.schabi.newpipe.extractor.utils

import java.util.Locale
import java.util.Optional

/**
 * This class contains a simple implementation of [Locale.forLanguageTag] for Android
 * API levels below 21 (Lollipop). This is needed as core library desugaring does not backport that
 * method as of this writing.
 *
 * Relevant issue: https://issuetracker.google.com/issues/171182330
 */
public object LocaleCompat {

    // Source: The AndroidX LocaleListCompat class's private forLanguageTagCompat() method.
    // Use Locale.forLanguageTag() on Android API level >= 21 / Java instead.
    // The Locale constructors are deprecated on Java 19+, but Locale.of() is not on Android.
    @Suppress("DEPRECATION")
    @JvmStatic
    public fun forLanguageTag(str: String): Optional<Locale> {
        val separator = when {
            str.contains("-") -> "-"
            str.contains("_") -> "_"
            else -> return Optional.of(Locale(str))
        }
        val args = str.split(separator)
        return when {
            args.size > 2 -> Optional.of(Locale(args[0], args[1], args[2]))
            args.size > 1 -> Optional.of(Locale(args[0], args[1]))
            args.size == 1 -> Optional.of(Locale(args[0]))
            else -> Optional.empty()
        }
    }
}
