package org.schabi.newpipe.extractor.localization

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

/**
 * Deserialisation does not run constructors: the codes that end up in request headers are
 * checked again, and the errors of an extraction are not saved with it.
 */
internal class LocalizationSerializationTest {

    @Test
    fun `valid codes survive a round trip`() {
        assertEquals(Localization("de", "AT"), roundTrip(Localization("de", "AT")))
        assertEquals(ContentCountry("419"), roundTrip(ContentCountry("419")))
    }

    @Test
    fun `a crafted saved state cannot put a line break in a header`() {
        // "en", as written in the stream (length then bytes), becomes "e\n"
        val crafted = replaceOnce(
            serialize(Localization("en", "GB")),
            byteArrayOf(0, 2, 'e'.code.toByte(), 'n'.code.toByte()),
            byteArrayOf(0, 2, 'e'.code.toByte(), '\n'.code.toByte()),
        )
        assertThrows(InvalidObjectException::class.java) { deserialize(crafted) }

        val country = replaceOnce(
            serialize(ContentCountry("GB")),
            byteArrayOf(0, 2, 'G'.code.toByte(), 'B'.code.toByte()),
            byteArrayOf(0, 2, 'G'.code.toByte(), '\r'.code.toByte()),
        )
        assertThrows(InvalidObjectException::class.java) { deserialize(country) }
    }

    @Test
    fun `the errors of an extraction are not saved`() {
        val info = StreamInfo(
            0, "https://www.youtube.com/watch?v=dQw4w9WgXcQ", "", StreamType.VIDEO_STREAM, "dQw4w9WgXcQ", "", 0,
        )
        info.addError(IllegalStateException("contains what the user watched"))

        val copy = roundTrip(info)

        assertTrue(copy.errors.isEmpty())
        copy.addError(IllegalStateException("still usable"))
        assertEquals(1, copy.errors.size)
    }

    private fun <T : Serializable> roundTrip(value: T): T {
        @Suppress("UNCHECKED_CAST")
        return deserialize(serialize(value)) as T
    }

    private fun serialize(value: Serializable): ByteArray =
        ByteArrayOutputStream().also { bytes -> ObjectOutputStream(bytes).use { it.writeObject(value) } }.toByteArray()

    private fun deserialize(bytes: ByteArray): Any = ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() }

    private fun replaceOnce(bytes: ByteArray, old: ByteArray, new: ByteArray): ByteArray {
        val at = (0..bytes.size - old.size).single { start -> old.indices.all { bytes[start + it] == old[it] } }
        return bytes.copyOf().also { new.copyInto(it, at) }
    }
}
