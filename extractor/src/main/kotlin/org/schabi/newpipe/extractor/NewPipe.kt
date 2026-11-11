/*
 * Created by Christian Schabesberger on 23.08.15.
 *
 * Copyright (C) 2015 Christian Schabesberger <chris.schabesberger@mailbox.org>
 * NewPipe Extractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import org.schabi.newpipe.extractor.utils.ExtractorLogger

/**
 * Provides access to streaming services supported by NewPipe.
 *
 * The downloader and the preferences are set by the app thread and read by the extraction
 * threads: they are replaced together in one volatile snapshot, so a reader always sees the
 * latest settings and never a new downloader with an old language, or a language with the
 * country of another. The static getters stay functions, as they were for Kotlin callers of the
 * Java class.
 */
public object NewPipe {
    private val TAG: String = NewPipe::class.java.simpleName

    private class Config(
        val downloader: Downloader?,
        val localization: Localization?,
        val contentCountry: ContentCountry?,
    )

    @Volatile
    private var config = Config(null, null, null)

    private val configLock = Any()

    private inline fun update(change: (Config) -> Config) {
        synchronized(configLock) { config = change(config) }
    }

    @JvmStatic
    public fun init(d: Downloader?) {
        ExtractorLogger.d(TAG, "Default init called")
        init(d, Localization.DEFAULT)
    }

    @JvmStatic
    public fun init(d: Downloader?, l: Localization) {
        ExtractorLogger.d(TAG, "Default init called with a localization")
        init(d, l, countryOf(l))
    }

    /** The language and country of the user are not logged. */
    @JvmStatic
    public fun init(d: Downloader?, l: Localization?, c: ContentCountry?) {
        // the downloader is an app object: its description is not logged
        ExtractorLogger.d(TAG, "Initializing")
        update { Config(d, l, c) }
    }

    @JvmStatic
    public fun getDownloader(): Downloader? = config.downloader

    // ////////////////////////////////////////////////////////////////////////
    // Utils
    // ////////////////////////////////////////////////////////////////////////

    @JvmStatic
    public fun getServices(): List<StreamingService> = ServiceList.all()

    @JvmStatic
    @Throws(ExtractionException::class)
    public fun getService(serviceId: Int): StreamingService =
        ServiceList.all().firstOrNull { it.serviceId == serviceId }
            ?: throw ExtractionException("There's no service with the id = \"$serviceId\"")

    @JvmStatic
    @Throws(ExtractionException::class)
    public fun getService(serviceName: String): StreamingService =
        ServiceList.all().firstOrNull { it.serviceInfo.name == serviceName }
            ?: throw ExtractionException("There's no service with this name")

    /** The link is untrusted and may carry personal data: it is not quoted in the exception. */
    @JvmStatic
    @Throws(ExtractionException::class)
    public fun getServiceByUrl(url: String): StreamingService {
        for (service in ServiceList.all()) {
            if (service.getLinkTypeByUrl(url) != StreamingService.LinkType.NONE) {
                return service
            }
        }
        throw ExtractionException("No service can handle the url")
    }

    // ////////////////////////////////////////////////////////////////////////
    // Localization
    // ////////////////////////////////////////////////////////////////////////

    @JvmStatic
    public fun setupLocalization(thePreferredLocalization: Localization) {
        setupLocalization(thePreferredLocalization, null)
    }

    @JvmStatic
    public fun setupLocalization(
        thePreferredLocalization: Localization,
        thePreferredContentCountry: ContentCountry?,
    ) {
        val country = thePreferredContentCountry ?: countryOf(thePreferredLocalization)
        update { Config(it.downloader, thePreferredLocalization, country) }
    }

    @JvmStatic
    public fun getPreferredLocalization(): Localization = config.localization ?: Localization.DEFAULT

    @JvmStatic
    public fun setPreferredLocalization(preferredLocalization: Localization?) {
        update { Config(it.downloader, preferredLocalization, it.contentCountry) }
    }

    @JvmStatic
    public fun getPreferredContentCountry(): ContentCountry =
        config.contentCountry ?: ContentCountry.DEFAULT

    @JvmStatic
    public fun setPreferredContentCountry(preferredContentCountry: ContentCountry?) {
        update { Config(it.downloader, it.localization, preferredContentCountry) }
    }

    private fun countryOf(localization: Localization): ContentCountry =
        if (localization.countryCode.isEmpty()) {
            ContentCountry.DEFAULT
        } else {
            ContentCountry(localization.countryCode)
        }
}
