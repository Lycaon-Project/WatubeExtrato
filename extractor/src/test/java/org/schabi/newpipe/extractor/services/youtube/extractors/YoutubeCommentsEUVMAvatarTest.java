package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grack.nanojson.JsonObject;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.localization.TimeAgoPatternsManager;

import java.util.List;

/**
 * The avatar of a comment author without an {@code avatar} object: its size comes from the
 * {@code =s<N>} parameter of its URL, and the URL itself is only handed over in https.
 */
class YoutubeCommentsEUVMAvatarTest {

    private static final String AVATAR = "yt3.ggpht.com/ytc/AIdro_abc";

    private static List<Image> avatarsOf(final String avatarThumbnailUrl)
            throws ParsingException {
        final JsonObject author = new JsonObject();
        if (avatarThumbnailUrl != null) {
            author.put("avatarThumbnailUrl", avatarThumbnailUrl);
        }
        final JsonObject payload = new JsonObject();
        payload.put("author", author);
        return new YoutubeCommentsEUVMInfoItemExtractor(new JsonObject(), null, payload,
                new JsonObject(), "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                TimeAgoPatternsManager.getTimeAgoParserFor(Localization.DEFAULT))
                .getUploaderAvatars();
    }

    private static Image onlyAvatarOf(final String avatarThumbnailUrl) throws ParsingException {
        final List<Image> avatars = avatarsOf(avatarThumbnailUrl);
        assertEquals(1, avatars.size());
        return avatars.get(0);
    }

    @Test
    void sizeIsReadFromTheUrl() throws ParsingException {
        final Image avatar = onlyAvatarOf("https://" + AVATAR + "=s88-c-k-c0x00ffffff-no-rj");
        assertEquals(88, avatar.getHeight());
        assertEquals(88, avatar.getWidth());
        assertEquals(Image.ResolutionLevel.LOW, avatar.getEstimatedResolutionLevel());
    }

    @Test
    void originalSizeOrOversizedValueIsUnknown() throws ParsingException {
        // =s0 asks for the original size, which the URL does not tell
        assertEquals(Image.HEIGHT_UNKNOWN,
                onlyAvatarOf("https://" + AVATAR + "=s0-c-k").getHeight());
        // more digits than any avatar size: never parsed, so it can not overflow
        final Image oversized = onlyAvatarOf("https://" + AVATAR + "=s99999999999999-c-k");
        assertEquals(Image.HEIGHT_UNKNOWN, oversized.getHeight());
        assertEquals(Image.WIDTH_UNKNOWN, oversized.getWidth());
        assertEquals(Image.ResolutionLevel.UNKNOWN, oversized.getEstimatedResolutionLevel());
    }

    @Test
    void urlIsOnlyHandedOverInHttps() throws ParsingException {
        assertEquals("https://" + AVATAR + "=s48",
                onlyAvatarOf("http://" + AVATAR + "=s48").getUrl());
        assertEquals("https://" + AVATAR + "=s48",
                onlyAvatarOf("//" + AVATAR + "=s48").getUrl());
    }

    @Test
    void missingUrlGivesNoAvatar() throws ParsingException {
        assertTrue(avatarsOf(null).isEmpty());
        assertTrue(avatarsOf("").isEmpty());
    }
}
