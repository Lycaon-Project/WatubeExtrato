package org.schabi.newpipe.downloader

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Mocks requests by using json files created by [RecordingDownloader]
 */
internal class MockDownloader(private val path: String) : Downloader() {

    private val mocks = HashMap<Request, Response>()

    init {
        try {
            Files.newDirectoryStream(Paths.get(path)) { entry ->
                entry.fileName.toString().startsWith(RecordingDownloader.FILE_NAME_PREFIX)
            }.use { directoryStream ->
                for (entry in directoryStream) {
                    Files.newBufferedReader(entry).use { reader ->
                        val response = MockGson.builder()
                            .create()
                            .fromJson(reader, TestRequestResponse::class.java)
                        mocks[response.request] = response.response
                    }
                }
            }
        } catch (ioe: IOException) {
            throw UncheckedIOException(ioe)
        }
    }

    override fun execute(request: Request): Response {
        return mocks[request] ?: throw NullPointerException(
            "No mock response for request with url '" + request.url() + "' exists in path '" +
                path + "'.\nPlease make sure to run the tests with the RecordingDownloader " +
                "first after changes.",
        )
    }
}
