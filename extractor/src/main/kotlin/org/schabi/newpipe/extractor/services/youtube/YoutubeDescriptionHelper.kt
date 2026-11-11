package org.schabi.newpipe.extractor.services.youtube

import org.jsoup.nodes.Entities
import org.schabi.newpipe.extractor.json.JsonObject
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getUrlFromNavigationEndpoint
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isYoutubeServiceURL
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.isYoutubeURL
import java.net.MalformedURLException
import java.net.URL
import java.util.function.Function
import java.util.regex.Pattern

public object YoutubeDescriptionHelper {

    private const val LINK_CLOSE = "</a>"
    private const val STRIKETHROUGH_OPEN = "<s>"
    private const val STRIKETHROUGH_CLOSE = "</s>"
    private const val BOLD_OPEN = "<b>"
    private const val BOLD_CLOSE = "</b>"
    private const val ITALIC_OPEN = "<i>"
    private const val ITALIC_CLOSE = "</i>"
    private val NO_BREAK_SPACE = Char(0x00A0)

    // special link chips (e.g. for YT videos, YT channels or social media accounts):
    // (u00a0) u00a0 u00a0 [/•] u00a0 <link content> u00a0 u00a0
    private val LINK_CONTENT_CLEANER_REGEX: Pattern = Pattern.compile("(?s)^ +[/•] +(.*?) +$")

    /**
     * Can be a command run, or a style run.
     */
    internal class Run @JvmOverloads constructor(
        @JvmField val open: String,
        @JvmField val close: String,
        @JvmField val pos: Int,
        @JvmField val transformContent: Function<String, String>? = null,
    ) {
        @JvmField var openPosInOutput: Int = -1

        fun sameOpen(other: Run): Boolean = open == other.open
    }

    /**
     * Parse a video description in the new "attributed" format, which contains the entire visible
     * plaintext (`content`) and an array of `commandRuns` and `styleRuns`.
     * Returns the formatted content in HTML format, and escapes the text to make sure there are no
     * XSS attacks.
     *
     * `commandRuns` include the links and their range in the text, while `styleRuns`
     * include the styling to apply to various ranges in the text.
     *
     * @param attributedDescription the JSON object of the attributed description
     * @return the parsed description, in HTML format, as a string
     */
    @JvmStatic
    public fun attributedDescriptionToHtml(attributedDescription: JsonObject?): String? {
        if (attributedDescription.isNullOrEmpty()) {
            return null
        }

        val content = attributedDescription.getString("content") ?: return null

        // all run pairs must always of length at least 1, or they should be discarded,
        // otherwise various assumptions made in runsToHtml may fail
        val openers = ArrayList<Run>()
        val closers = ArrayList<Run>()
        addAllCommandRuns(attributedDescription, content.length, openers, closers)
        addAllStyleRuns(attributedDescription, content.length, openers, closers)

        // Note that sorting this way might put closers with the same close position in the wrong
        // order with respect to their openers, causing unnecessary closes and reopens. E.g.
        // <b>b<i>b&i</i></b> is instead generated as <b>b<i>b&i</b></i><b></b> if the </b> is
        // encountered before the </i>. Solving this wouldn't be difficult, thanks to stable sort,
        // but would require additional sorting steps which would just make this slower for the
        // general case where it's unlikely there are coincident closes.
        openers.sortBy { it.pos }
        closers.sortBy { it.pos }

        return runsToHtml(openers, closers, content)
    }

    /**
     * Applies the formatting specified by the intervals stored in `openers` and `closers` to
     * `content` in order to obtain valid HTML even when intervals overlap. For
     * example &lt;b&gt;b&lt;i&gt;b&i&lt;/b&gt;i&lt;/i&gt; would not be valid HTML, so this function
     * instead generates &lt;b&gt;b&lt;i&gt;b&i&lt;/i&gt;&lt;/b&gt;&lt;i&gt;i&lt;/i&gt;. Any HTML
     * special characters in `rawContent` are escaped to make sure there are no XSS attacks.
     *
     * Every opener in `openers` must have a corresponding closer in `closers`. Every
     * corresponding (opener, closer) pair must have a length of at least one (i.e. empty intervals
     * are not allowed).
     *
     * @param openers    contains all of the places where a run begins, must have the same size of
     *                   closers, must be ordered by [Run.pos]
     * @param closers    contains all of the places where a run ends, must have the same size of
     *                   openers, must be ordered by [Run.pos]
     * @param rawContent the content to apply formatting to, and to escape to avoid XSS
     * @return the formatted content in HTML
     */
    @JvmStatic
    @JvmName("runsToHtml")
    internal fun runsToHtml(openers: List<Run>, closers: List<Run>, rawContent: String): String {
        val content = rawContent.replace(NO_BREAK_SPACE, ' ')
        val openRuns = ArrayDeque<Run>()
        val tempStack = ArrayDeque<Run>()
        val textBuilder = StringBuilder()
        var currentTextPos = 0
        var openersIndex = 0
        var closersIndex = 0

        // openers and closers have the same length, but we will surely finish openers earlier than
        // closers, since every opened interval needs to be closed at some point and there can't be
        // empty intervals, hence check only closersIndex < closers.size()
        while (closersIndex < closers.size) {
            val minPos = if (openersIndex < openers.size) {
                minOf(closers[closersIndex].pos, openers[openersIndex].pos)
            } else {
                closers[closersIndex].pos
            }

            // append piece of text until current index
            textBuilder.append(Entities.escape(content.substring(currentTextPos, minPos)))
            currentTextPos = minPos

            if (closers[closersIndex].pos == minPos) {
                // even in case of position tie, first process closers
                val closer = closers[closersIndex]
                ++closersIndex
                closeRun(closer, openRuns, tempStack, textBuilder)
            } else {
                // this will never be reached if openersIndex >= openers.size() because of the
                // way minPos is calculated
                val opener = openers[openersIndex]
                textBuilder.append(opener.open)
                opener.openPosInOutput = textBuilder.length // save for transforming later
                openRuns.addLast(opener)
                ++openersIndex
            }
        }

        // append last piece of text
        textBuilder.append(Entities.escape(content.substring(currentTextPos)))

        return textBuilder.toString()
            .replace("\n", "<br>")
            .replace("  ", " &nbsp;")
    }

    private fun closeRun(
        closer: Run,
        openRuns: ArrayDeque<Run>,
        tempStack: ArrayDeque<Run>,
        textBuilder: StringBuilder,
    ) {
        // because of the assumptions, this while wouldn't need the !openRuns.empty()
        // condition, because no run will close before being opened, but let's be sure
        while (openRuns.isNotEmpty()) {
            val popped = openRuns.removeLast()
            if (popped.sameOpen(closer)) {
                // before closing the current run, if the run has a transformContent
                // function, use it to transform the content of the current run, based on
                // the openPosInOutput set when the current run was opened
                val transform = popped.transformContent
                if (transform != null && popped.openPosInOutput >= 0) {
                    textBuilder.replace(
                        popped.openPosInOutput,
                        textBuilder.length,
                        transform.apply(textBuilder.substring(popped.openPosInOutput)),
                    )
                }
                // close the run that we really need to close
                textBuilder.append(popped.close)
                break
            }
            // we keep popping from openRuns, closing all of the runs we find,
            // until we find the run that we really need to close ...
            textBuilder.append(popped.close)
            tempStack.addLast(popped)
        }
        while (tempStack.isNotEmpty()) {
            // ... and then we reopen all of the runs that we didn't need to close
            // e.g. in <b>b<i>b&i</b>i</i>, when </b> is encountered, </i></b><i> is printed
            // instead, to make sure the HTML is valid, obtaining <b>b<i>b&i</i></b><i>i</i>
            val popped = tempStack.removeLast()
            textBuilder.append(popped.open)
            openRuns.addLast(popped)
        }
    }

    /**
     * @param start         where a run starts in the content, as given by YouTube
     * @param length        how long the run is, as given by YouTube
     * @param contentLength how long the content is
     * @return where the run ends, inside the content, or -1 if it has no character in it
     */
    private fun runEnd(start: Int, length: Int, contentLength: Int): Int {
        if (start < 0 || length < 1 || start >= contentLength) {
            return -1
        }
        // long: a huge length must not wrap around to a negative end
        return minOf(start.toLong() + length, contentLength.toLong()).toInt()
    }

    // URL(String) is deprecated on Java 20+, but URI parsing is stricter: keep java.net.URL
    @Suppress("DEPRECATION")
    @Throws(MalformedURLException::class)
    private fun urlOf(spec: String): URL = URL(spec)

    private fun addAllCommandRuns(
        attributedDescription: JsonObject,
        contentLength: Int,
        openers: MutableList<Run>,
        closers: MutableList<Run>,
    ) {
        for (item in attributedDescription.getArray("commandRuns")) {
            val run = item as? JsonObject ?: continue
            val navigationEndpoint = run.getObject("onTap").getObject("innertubeCommand")

            val startIndex = run.getInt("startIndex", -1)
            val endIndex = runEnd(startIndex, run.getInt("length", 0), contentLength)
            if (endIndex < 0) {
                continue
            }

            val url = getUrlFromNavigationEndpoint(navigationEndpoint) ?: continue

            val isYoutubeUrl = try {
                val parsedUrl = urlOf(url)
                isYoutubeURL(parsedUrl) || isYoutubeServiceURL(parsedUrl)
            } catch (_: MalformedURLException) {
                // this should never happen, but just in case, assume this is a generic URL
                false
            }

            val open = "<a href=\"" + Entities.escape(url) + "\">"
            val transformFun = getTransformContentFun(run, isYoutubeUrl)
            val transformContent = Function<String, String> { transformFun(it) }

            openers.add(Run(open, LINK_CLOSE, startIndex, transformContent))
            closers.add(Run(open, LINK_CLOSE, endIndex, transformContent))
        }
    }

    private fun getTransformContentFun(run: JsonObject, isYoutube: Boolean): (String) -> String {
        val accessibilityLabel = run.getObject("onTapOptions")
            .getObject("accessibilityInfo")
            .getString("accessibilityLabel", "")
            .orEmpty()
            // accessibility labels are e.g. "Instagram Channel Link: instagram_profile_name"
            .replaceFirst(" Channel Link", "")

        return if (isYoutube || accessibilityLabel.isEmpty() ||
            accessibilityLabel.startsWith("YouTube: ")
        ) {
            // if there is no accessibility label, or the link points to YouTube, cleanup the link
            // text, see LINK_CONTENT_CLEANER_REGEX's documentation for more details
            val cleanLinkText: (String) -> String = { content ->
                val m = LINK_CONTENT_CLEANER_REGEX.matcher(content)
                if (m.find()) m.group(1) else content
            }
            cleanLinkText
        } else {
            // if there is an accessibility label, replace the link text with it, because on the
            // YouTube website an ambiguous link text is next to an icon explaining which service it
            // belongs to, but since we can't add icons, we instead use the accessibility label
            // which contains information about the service.
            // The label comes from YouTube and goes into HTML: it is escaped like the content.
            val escapedLabel: String = Entities.escape(accessibilityLabel)
            val replaceWithLabel: (String) -> String = { _ -> escapedLabel }
            replaceWithLabel
        }
    }

    private fun addAllStyleRuns(
        attributedDescription: JsonObject,
        contentLength: Int,
        openers: MutableList<Run>,
        closers: MutableList<Run>,
    ) {
        for (item in attributedDescription.getArray("styleRuns")) {
            val run = item as? JsonObject ?: continue
            val start = run.getInt("startIndex", -1)
            val end = runEnd(start, run.getInt("length", 0), contentLength)
            if (end < 0) {
                continue
            }

            if (run.has("strikethrough")) {
                openers.add(Run(STRIKETHROUGH_OPEN, STRIKETHROUGH_CLOSE, start))
                closers.add(Run(STRIKETHROUGH_OPEN, STRIKETHROUGH_CLOSE, end))
            }

            if (run.getBoolean("italic", false)) {
                openers.add(Run(ITALIC_OPEN, ITALIC_CLOSE, start))
                closers.add(Run(ITALIC_OPEN, ITALIC_CLOSE, end))
            }

            if (run.has("weightLabel") && run.getString("weightLabel") != "FONT_WEIGHT_NORMAL") {
                openers.add(Run(BOLD_OPEN, BOLD_CLOSE, start))
                closers.add(Run(BOLD_OPEN, BOLD_CLOSE, end))
            }
        }
    }
}
