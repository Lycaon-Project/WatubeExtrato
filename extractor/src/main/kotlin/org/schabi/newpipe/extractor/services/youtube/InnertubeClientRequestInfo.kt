package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.DESKTOP_CLIENT_PLATFORM
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.MOBILE_CLIENT_PLATFORM
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_CLIENT_ID
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_CLIENT_NAME
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_CLIENT_VERSION
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_DEVICE_MODEL
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.VISIONOS_VERSION
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WATCH_CLIENT_SCREEN
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_CLIENT_ID
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_CLIENT_NAME
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_HARDCODED_CLIENT_VERSION
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_MUSIC_ANALYTICS_CLIENT_ID
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_MUSIC_ANALYTICS_CLIENT_NAME
import org.schabi.newpipe.extractor.services.youtube.ClientsConstants.WEB_MUSIC_ANALYTICS_CLIENT_VERSION

// TODO: add docs

public class InnertubeClientRequestInfo private constructor(
    @JvmField public var clientInfo: ClientInfo,
    @JvmField public var deviceInfo: DeviceInfo,
) {

    public class ClientInfo internal constructor(
        @JvmField public var clientName: String,
        @JvmField public var clientVersion: String,
        @JvmField public var clientId: String,
        @JvmField public var clientScreen: String?,
        @JvmField public var visitorData: String?,
    )

    public class DeviceInfo internal constructor(
        @JvmField public var platform: String?,
        @JvmField public var deviceMake: String?,
        @JvmField public var deviceModel: String?,
        @JvmField public var osName: String?,
        @JvmField public var osVersion: String?,
        @JvmField public var androidSdkVersion: Int,
    )

    public companion object {
        @JvmStatic
        public fun ofWebClient(): InnertubeClientRequestInfo = InnertubeClientRequestInfo(
            ClientInfo(
                WEB_CLIENT_NAME, WEB_HARDCODED_CLIENT_VERSION, WEB_CLIENT_ID,
                WATCH_CLIENT_SCREEN, null,
            ),
            DeviceInfo(DESKTOP_CLIENT_PLATFORM, null, null, null, null, -1),
        )

        @JvmStatic
        public fun ofWebMusicAnalyticsChartsClient(): InnertubeClientRequestInfo =
            InnertubeClientRequestInfo(
                ClientInfo(
                    WEB_MUSIC_ANALYTICS_CLIENT_NAME, WEB_MUSIC_ANALYTICS_CLIENT_VERSION,
                    WEB_MUSIC_ANALYTICS_CLIENT_ID, null, null,
                ),
                DeviceInfo(null, null, null, null, null, -1),
            )

        @JvmStatic
        public fun ofVisionOsClient(): InnertubeClientRequestInfo = InnertubeClientRequestInfo(
            ClientInfo(
                VISIONOS_CLIENT_NAME, VISIONOS_CLIENT_VERSION, VISIONOS_CLIENT_ID,
                WATCH_CLIENT_SCREEN, null,
            ),
            DeviceInfo(
                MOBILE_CLIENT_PLATFORM, "Apple", VISIONOS_DEVICE_MODEL, "visionOS",
                VISIONOS_VERSION, -1,
            ),
        )
    }
}
