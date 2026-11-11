package org.schabi.newpipe.downloader

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.services.soundcloud.SoundcloudParsingHelper
import org.schabi.newpipe.extractor.utils.Utils
import java.util.Locale

object DownloaderFactory {

    private val DEFAULT_DOWNLOADER = DownloaderType.REAL

    private var cachedDownloaderType: DownloaderType? = null

    /** The SoundCloud client ID in every recorded SoundCloud request. */
    private const val MOCKED_SOUNDCLOUD_CLIENT_ID = "CkCiIyf14rHi27fhk7HxhPOzc85okfSJ"

    private fun getDownloaderType(): DownloaderType {
        val cached = cachedDownloaderType
        if (cached != null) {
            return cached
        }
        val determined = determineDownloaderType()
        cachedDownloaderType = determined
        return determined
    }

    private fun determineDownloaderType(): DownloaderType {
        var propValue = System.getProperty("downloader")
        if (Utils.isNullOrEmpty(propValue)) {
            return DEFAULT_DOWNLOADER
        }
        propValue = propValue.uppercase()
        // Use shortcut because RECORDING is quite long
        if (propValue == "REC") {
            return DownloaderType.RECORDING
        }
        try {
            return DownloaderType.valueOf(propValue)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Unknown downloader name: $propValue", e)
        }
    }

    @JvmStatic
    fun getDownloader(clazz: Class<*>): Downloader = getDownloader(getMockPath(clazz, null))

    @JvmStatic
    fun getDownloader(clazz: Class<*>, specificUseCase: String?): Downloader =
        getDownloader(getMockPath(clazz, specificUseCase))

    /**
     * Always returns a path without a trailing '/', so that it can be used both as a folder name
     * and as a filename. The [MockDownloader] will use it as a folder name, but other tests
     * can use it as a filename, if only one custom mock file is needed for that test.
     */
    @JvmStatic
    fun getMockPath(clazz: Class<*>, specificUseCase: String?): String {
        var baseName = clazz.name
        if (specificUseCase != null) {
            baseName += ".$specificUseCase"
        }
        return "src/test/resources/mocks/v1/" +
            baseName
                .lowercase(Locale.ENGLISH)
                .replace('$', '.')
                .replace("test", "")
                .replace('.', '/')
    }

    /**
     * Returns a implementation of a [Downloader].
     *
     * If the system property "downloader" is set and is one of [DownloaderType],
     * then a downloader of that type is returned.
     * It can be passed in with gradle by adding the argument -Ddownloader=abcd,
     * where abcd is one of [DownloaderType]
     *
     * Otherwise it falls back to [DEFAULT_DOWNLOADER].
     * Change this during development on the local machine to use a different downloader.
     *
     * @param path The path to the folder where mocks are saved/retrieved.
     */
    @JvmStatic
    internal fun getDownloader(path: String): Downloader =
        when (getDownloaderType()) {
            DownloaderType.REAL -> DownloaderTestImpl.getInstance()
            DownloaderType.MOCK -> {
                SoundcloudParsingHelper.setClientIdForTests(MOCKED_SOUNDCLOUD_CLIENT_ID)
                MockDownloader(path)
            }
            DownloaderType.RECORDING -> RecordingDownloader(path)
        }
}
