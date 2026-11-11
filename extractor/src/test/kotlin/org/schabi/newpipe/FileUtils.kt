package org.schabi.newpipe

import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.json.JsonWriter
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Util class to write file to disk
 *
 * Can be used to debug and test, for example writing a service's JSON response
 * (especially useful if the response provided by the service is not documented)
 */
object FileUtils {

    @JvmStatic
    @Throws(IOException::class)
    fun createFile(path: String, content: JsonObject) {
        createFile(path, jsonObjToString(content))
    }

    @JvmStatic
    @Throws(IOException::class)
    fun createFile(path: String, array: JsonArray) {
        createFile(path, jsonArrayToString(array))
    }

    /**
     * Create a file given a path and its content. Create subdirectories if needed
     *
     * @param path    the path to write the file, including the filename (and its extension)
     * @param content the content to write
     * @throws IOException if the directories or the file cannot be written
     */
    @JvmStatic
    @Throws(IOException::class)
    fun createFile(path: String, content: String) {
        // like Java's String.split: trailing empty strings are dropped
        val dirs = path.split("/").dropLastWhile { it.isEmpty() }
        if (dirs.size > 1) {
            val pathWithoutFileName = path.replace(dirs.last(), "")
            if (!Files.exists(Paths.get(pathWithoutFileName))) { // create dirs if they don't exist
                if (!File(pathWithoutFileName).mkdirs()) {
                    throw IOException("An error occurred while creating directories")
                }
            }
        }
        writeFile(path, content)
    }

    /**
     * Write a file to disk
     *
     * @param filename the file name (and its extension if wanted)
     * @param content  the content to write
     * @throws IOException if the file cannot be written
     */
    @Throws(IOException::class)
    private fun writeFile(filename: String, content: String) {
        File(filename).writeText(content)
    }

    /**
     * Resolves the test resource file based on its filename. Looks in
     * `extractor/src/test/resources/` and `src/test/resources/`
     * @param filename the resource filename
     * @return the resource file
     */
    @JvmStatic
    fun resolveTestResource(filename: String): File {
        val file = File("extractor/src/test/resources/$filename")
        return if (file.exists()) file else File("src/test/resources/$filename")
    }

    /**
     * Convert a JSON object to String
     * toString() does not produce a valid JSON string
     */
    @JvmStatic
    fun jsonObjToString(`object`: JsonObject): String = JsonWriter.string(`object`)

    /**
     * Convert a JSON array to String
     * toString() does not produce a valid JSON string
     */
    @JvmStatic
    fun jsonArrayToString(array: JsonArray): String = JsonWriter.string(array)
}
