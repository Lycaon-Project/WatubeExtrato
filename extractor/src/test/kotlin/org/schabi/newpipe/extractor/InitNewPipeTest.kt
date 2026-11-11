package org.schabi.newpipe.extractor

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.schabi.newpipe.downloader.DownloaderFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
interface InitNewPipeTest {

    @BeforeAll
    @Throws(Exception::class)
    fun setUp() {
        initNewPipe(this.javaClass)
    }

    companion object {
        @JvmStatic
        fun initNewPipe(clazz: Class<*>) {
            NewPipe.init(DownloaderFactory.getDownloader(clazz))
        }

        @JvmStatic
        fun initNewPipe(clazz: Class<*>, specificUseCase: String?) {
            NewPipe.init(DownloaderFactory.getDownloader(clazz, specificUseCase))
        }

        /**
         * Ensures that NewPipe is not initialized.
         * This can be used to ensure that a class works standalone and does e.g. no network requests.
         */
        @JvmStatic
        fun initEmpty() {
            NewPipe.init(null, null, null)
        }
    }
}
