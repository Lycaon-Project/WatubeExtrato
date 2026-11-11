package org.schabi.newpipe.extractor.services.youtube

import org.schabi.newpipe.extractor.MetaInfo
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.json.JsonArray
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.extractCachedUrlIfNeeded
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObjectOrThrow
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isGoogleURL
import org.schabi.newpipe.extractor.stream.Description
import org.schabi.newpipe.extractor.utils.SecureUrls
import org.schabi.newpipe.extractor.utils.Utils
import java.net.MalformedURLException
import java.net.URL

public object YoutubeMetaInfoHelper {

    @JvmStatic
    @Throws(ParsingException::class)
    public fun getMetaInfo(contents: JsonArray): List<MetaInfo> {
        val metaInfo = ArrayList<MetaInfo>()
        for (content in contents) {
            // an item which is not an object (tampered data) holds no meta info
            val resultObject = content as? JsonObject ?: continue
            if (!resultObject.has("itemSectionRenderer")) {
                continue
            }
            for (sectionContentObject in resultObject.getObject("itemSectionRenderer").getArray("contents")) {
                val sectionContent = sectionContentObject as? JsonObject ?: continue
                if (sectionContent.has("infoPanelContentRenderer")) {
                    metaInfo.add(getInfoPanelContent(sectionContent.getObject("infoPanelContentRenderer")))
                }
                if (sectionContent.has("clarificationRenderer")) {
                    metaInfo.add(getClarificationRenderer(sectionContent.getObject("clarificationRenderer")))
                }
                if (sectionContent.has("emergencyOneboxRenderer")) {
                    getEmergencyOneboxRenderer(
                        sectionContent.getObject("emergencyOneboxRenderer"),
                        metaInfo,
                    )
                }
            }
        }
        return metaInfo
    }

    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    @Throws(MalformedURLException::class)
    private fun urlOf(spec: String): URL = URL(spec)

    /**
     * The link of a meta info, shown to the user and opened when tapped: an http(s) link only
     * (a `file:` or `jar:` one would open local data), never quoted in the messages.
     */
    @Throws(ParsingException::class)
    private fun requireWebUrl(url: String?, error: String): URL {
        if (url == null || !SecureUrls.isWebLink(url)) {
            throw ParsingException(error)
        }
        return try {
            urlOf(url)
        } catch (_: MalformedURLException) {
            // not chained: its message quotes the URL
            throw ParsingException(error)
        }
    }

    @Throws(ParsingException::class)
    private fun getInfoPanelContent(infoPanelContentRenderer: JsonObject): MetaInfo {
        val metaInfo = MetaInfo()
        val sb = StringBuilder()
        for (paragraph in infoPanelContentRenderer.getArray("paragraphs")) {
            if (sb.isNotEmpty()) {
                sb.append("<br>")
            }
            // the HTML form escapes the text of every run: the content is an HTML description
            sb.append(getTextFromObject(paragraph as? JsonObject, true).orEmpty())
        }
        metaInfo.content = Description.of(sb.toString(), Description.Type.HTML)
        if (infoPanelContentRenderer.has("sourceEndpoint")) {
            val metaInfoLinkUrl = getUrlFromNavigationEndpoint(
                infoPanelContentRenderer.getObject("sourceEndpoint"),
            )
            metaInfo.addUrl(
                requireWebUrl(extractCachedUrlIfNeeded(metaInfoLinkUrl), "Could not get metadata info URL"),
            )

            val metaInfoLinkText = if (infoPanelContentRenderer.has("inlineSource")) {
                getTextFromObject(infoPanelContentRenderer.getObject("inlineSource"))
            } else {
                getTextFromObject(infoPanelContentRenderer.getObject("disclaimer"))
            }
            if (metaInfoLinkText.isNullOrEmpty()) {
                throw ParsingException("Could not get metadata info link text.")
            }
            metaInfo.addUrlText(metaInfoLinkText)
        }

        return metaInfo
    }

    @Throws(ParsingException::class)
    private fun getClarificationRenderer(clarificationRenderer: JsonObject): MetaInfo {
        val metaInfo = MetaInfo()

        val title = getTextFromObject(clarificationRenderer.getObject("contentTitle"))
        val text = getTextFromObject(clarificationRenderer.getObject("text"))
        if (title == null || text == null) {
            throw ParsingException("Could not extract clarification renderer content")
        }
        metaInfo.title = title
        metaInfo.content = Description(text, Description.Type.PLAIN_TEXT)

        if (clarificationRenderer.has("actionButton")) {
            val actionButton = clarificationRenderer.getObject("actionButton")
                .getObject("buttonRenderer")
            val url = getUrlFromNavigationEndpoint(actionButton.getObject("command"))
            metaInfo.addUrl(requireWebUrl(extractCachedUrlIfNeeded(url), "Could not get metadata info URL"))

            val metaInfoLinkText = getTextFromObject(actionButton.getObject("text"))
            if (metaInfoLinkText.isNullOrEmpty()) {
                throw ParsingException("Could not get metadata info link text.")
            }
            metaInfo.addUrlText(metaInfoLinkText)
        }

        if (clarificationRenderer.has("secondaryEndpoint") &&
            clarificationRenderer.has("secondarySource")
        ) {
            val url = getUrlFromNavigationEndpoint(clarificationRenderer.getObject("secondaryEndpoint"))
            // Ignore Google URLs, because those point to a Google search about "Covid-19"
            if (url != null && !isGoogleURL(url)) {
                metaInfo.addUrl(requireWebUrl(url, "Could not get metadata info secondary URL"))
                val description = getTextFromObject(clarificationRenderer.getObject("secondarySource"))
                metaInfo.addUrlText(description ?: url)
            }
        }

        return metaInfo
    }

    @Throws(ParsingException::class)
    private fun getEmergencyOneboxRenderer(
        emergencyOneboxRenderer: JsonObject,
        metaInfoList: MutableList<MetaInfo>,
    ) {
        val supportRenderers = emergencyOneboxRenderer.values
            .filterIsInstance<JsonObject>()
            .filter { it.has("singleActionEmergencySupportRenderer") }
            .map { it.getObject("singleActionEmergencySupportRenderer") }

        if (supportRenderers.isEmpty()) {
            throw ParsingException("Could not extract any meta info from emergency renderer")
        }

        for (r in supportRenderers) {
            val metaInfo = MetaInfo()

            // usually an encouragement like "We are with you"
            val title = getTextFromObjectOrThrow(r.getObject("title"), "title")

            // usually a phone number
            // this variable is expected to start with "\n"
            val action = when {
                r.has("actionText") -> "\n" + getTextFromObjectOrThrow(r.getObject("actionText"), "action")

                r.has("contacts") -> buildString {
                    // Loop over contacts item from the first contact to the last one
                    for (contact in r.getArray("contacts")) {
                        append("\n")
                        append(
                            getTextFromObjectOrThrow(
                                (contact as? JsonObject)?.getObject("actionText"),
                                "contacts.actionText",
                            ),
                        )
                    }
                }

                else -> ""
            }

            // usually details about the phone number
            val details = getTextFromObjectOrThrow(r.getObject("detailsText"), "details")

            // usually the name of an association
            val urlText = getTextFromObjectOrThrow(r.getObject("navigationText"), "urlText")

            metaInfo.title = title
            metaInfo.content = Description(details + action, Description.Type.PLAIN_TEXT)
            metaInfo.addUrlText(urlText)

            // usually the webpage of the association
            val url = getUrlFromNavigationEndpoint(r.getObject("navigationEndpoint"))
                ?: throw ParsingException("Could not extract emergency renderer url")

            metaInfo.addUrl(
                requireWebUrl(Utils.replaceHttpWithHttps(url) ?: url, "Could not parse emergency renderer url"),
            )

            metaInfoList.add(metaInfo)
        }
    }
}
