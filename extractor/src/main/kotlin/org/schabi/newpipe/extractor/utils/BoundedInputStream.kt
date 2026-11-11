package org.schabi.newpipe.extractor.utils

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Reads at most [limit] bytes of [input] and fails past them, so that a file picked by the user
 * (a crafted archive that inflates to gigabytes, an endless stream) cannot fill the memory or
 * keep the import busy. Closing it does not close [input]: the entries of a zip archive are read
 * one after another through the same stream.
 */
internal class BoundedInputStream(input: InputStream, private val limit: Long) : FilterInputStream(input) {

    private var count = 0L

    override fun read(): Int {
        val value = super.read()
        if (value != -1) {
            counted(1)
        }
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = super.read(buffer, offset, length)
        if (read > 0) {
            counted(read.toLong())
        }
        return read
    }

    /** Read through, so that the skipped bytes count too. */
    override fun skip(n: Long): Long {
        val buffer = ByteArray(minOf(n, SKIP_BUFFER_SIZE.toLong()).toInt().coerceAtLeast(1))
        var skipped = 0L
        while (skipped < n) {
            val read = read(buffer, 0, minOf(buffer.size.toLong(), n - skipped).toInt())
            if (read == -1) {
                break
            }
            skipped += read
        }
        return skipped
    }

    /** Reads what is left: until the end of the current zip entry, for instance. */
    fun drain() {
        while (skip(SKIP_BUFFER_SIZE.toLong()) > 0) {
            // counted by skip()
        }
    }

    override fun markSupported(): Boolean = false

    override fun close() {
        // the underlying stream belongs to the caller
    }

    private fun counted(read: Long) {
        count += read
        if (count > limit) {
            throw IOException("Input larger than $limit bytes")
        }
    }

    private companion object {
        private const val SKIP_BUFFER_SIZE = 8 * 1024
    }
}
