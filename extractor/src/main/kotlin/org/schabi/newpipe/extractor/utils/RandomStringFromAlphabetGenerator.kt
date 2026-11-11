package org.schabi.newpipe.extractor.utils

import java.util.Random

/**
 * Generates a random string from a predefined alphabet.
 */
public object RandomStringFromAlphabetGenerator {

    /**
     * Generate a random string from an alphabet.
     *
     * @param alphabet the characters' alphabet to use
     * @param length   the length of the returned string (greater than 0)
     * @param random   [Random] (or better [java.security.SecureRandom]) used for
     * generating the random string
     * @return a random string of the requested length made of only characters from the provided
     * alphabet
     */
    @JvmStatic
    public fun generate(alphabet: String, length: Int, random: Random): String {
        val stringBuilder = StringBuilder(length)
        repeat(length) {
            stringBuilder.append(alphabet[random.nextInt(alphabet.length)])
        }
        return stringBuilder.toString()
    }
}
