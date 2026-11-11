package org.schabi.newpipe.downloader

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Relays requests to [DownloaderTestImpl] and saves the request/response pair into a json
 * file.
 *
 * Those files are used by [MockDownloader].
 *
 * The files **must** be created on the local dev environment
 * and recreated when the requests made by a test class change.
 *
 * Run the test class as a whole and not each test separately.
 * Make sure the requests made by a class are unique.
 */
internal class RecordingDownloader(stringPath: String) : Downloader() {

    private var index = 0
    private val path = Paths.get(stringPath)

    /**
     * Creates the folder described by `stringPath` if it does not exist.
     * Deletes existing files starting with [RecordingDownloader.FILE_NAME_PREFIX].
     * @param stringPath Path to the folder where the json files will be saved to.
     */
    init {
        if (Files.exists(path)) {
            try {
                Files.newDirectoryStream(path) { entry ->
                    entry.fileName.toString().startsWith(FILE_NAME_PREFIX)
                }.use { directoryStream ->
                    for (entry in directoryStream) {
                        Files.delete(entry)
                    }
                }
            } catch (ioe: IOException) {
                throw UncheckedIOException(ioe)
            }
        } else {
            try {
                Files.createDirectories(path)
            } catch (e: IOException) {
                throw UncheckedIOException(e)
            }
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val downloader: Downloader = DownloaderTestImpl.getInstance()
        var response = downloader.execute(request)
        response = Response(
            response.responseCode(),
            response.responseMessage(),
            response.responseHeaders(),
            response.responseBody().replace(IP_V4_REGEX, "127.0.0.1"),
            response.latestUrl(),
        )

        val outputPath = path.resolve(FILE_NAME_PREFIX + index + ".json")
        index++
        Files.newBufferedWriter(outputPath).use { writer ->
            MockGson.builder()
                .setPrettyPrinting()
                .create()
                .toJson(TestRequestResponse(request, response), writer)
        }

        return response
    }

    companion object {
        const val FILE_NAME_PREFIX = "generated_mock_"

        // From https://stackoverflow.com/a/15875500/13516981
        private val IP_V4_REGEX = Regex(
            "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}" +
                "(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)",
        )
    }
}
