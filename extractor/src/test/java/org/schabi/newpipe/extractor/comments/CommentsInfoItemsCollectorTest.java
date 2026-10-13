package org.schabi.newpipe.extractor.comments;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.util.List;

class CommentsInfoItemsCollectorTest {

    private static CommentsInfoItemExtractor comment(final boolean uploaderVerified) {
        return new CommentsInfoItemExtractor() {
            @Override
            public boolean isUploaderVerified() {
                return uploaderVerified;
            }

            @Override
            public String getName() {
                return "comment";
            }

            @Override
            public String getUrl() {
                return "https://www.youtube.com/watch?v=dQw4w9WgXcQ";
            }

            @Override
            public List<Image> getThumbnails() {
                return List.of();
            }
        };
    }

    @Test
    void uploaderVerifiedIsCopiedToTheItem() throws ParsingException {
        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(0);
        assertTrue(collector.extract(comment(true)).isUploaderVerified());
        assertFalse(collector.extract(comment(false)).isUploaderVerified());
    }
}
