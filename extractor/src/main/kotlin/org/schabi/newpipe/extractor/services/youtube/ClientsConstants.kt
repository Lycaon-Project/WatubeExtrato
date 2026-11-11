package org.schabi.newpipe.extractor.services.youtube

internal object ClientsConstants {

    // Common client fields

    const val DESKTOP_CLIENT_PLATFORM: String = "DESKTOP"
    const val MOBILE_CLIENT_PLATFORM: String = "MOBILE"
    const val WATCH_CLIENT_SCREEN: String = "WATCH"

    // WEB (YouTube desktop) client fields

    const val WEB_CLIENT_ID: String = "1"
    const val WEB_CLIENT_NAME: String = "WEB"

    /**
     * The client version for InnerTube requests with the `WEB` client, used as the last
     * fallback if the extraction of the real one failed.
     */
    const val WEB_HARDCODED_CLIENT_VERSION: String = "2.20260805.01.00"

    // WEB_REMIX (YouTube Music) client fields

    const val WEB_REMIX_CLIENT_ID: String = "67"
    const val WEB_REMIX_CLIENT_NAME: String = "WEB_REMIX"
    const val WEB_REMIX_HARDCODED_CLIENT_VERSION: String = "1.20260804.16.00"

    // WEB_MUSIC_ANALYTICS (YouTube charts)

    const val WEB_MUSIC_ANALYTICS_CLIENT_ID: String = "31"
    const val WEB_MUSIC_ANALYTICS_CLIENT_NAME: String = "WEB_MUSIC_ANALYTICS"
    const val WEB_MUSIC_ANALYTICS_CLIENT_VERSION: String = "2.0"

    // visionOS client fields

    const val VISIONOS_CLIENT_ID: String = "101"
    const val VISIONOS_CLIENT_NAME: String = "VISIONOS"

    // See https://apps.apple.com/us/app/youtube-for-visionos/id6745572359 for the latest version
    const val VISIONOS_CLIENT_VERSION: String = "1.04"

    // See https://theapplewiki.com/wiki/Apple_Vision_Pro_(M5)
    const val VISIONOS_DEVICE_MODEL: String = "RealityDevice17,1"

    // See https://theapplewiki.com/wiki/Firmware/Apple_Vision/26.x
    const val VISIONOS_VERSION: String = "26.6.0.23O770"
    const val VISIONOS_USER_AGENT_VERSION: String = "26_6_0"
}
