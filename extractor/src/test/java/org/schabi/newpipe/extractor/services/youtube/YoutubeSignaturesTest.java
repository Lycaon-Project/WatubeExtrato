package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import javax.annotation.Nonnull;

class YoutubeSignaturesTest implements InitYoutubeTest {
    /*
    The first column of the CSV entries is a video ID
    The second one of these entries are not real signatures, but as the deobfuscation function
    manipulates strings, we can use random characters combined as strings to test the extraction
    and the execution of the function
     */
    @CsvSource(value = {
            "QzUGs1qRTEI,5QjJrWzVcOutYYNyxkDJVkzQDZQxNbbxGi4hRoh2h4PomQMQq9vo2WPHVpHgxRn7qT3WyhRiJa1k1t1DL3lynZtupHmG3wW4qh59faKjtY4UVu",
            ",7vIK4hG6NbcIEQP4ZIRjonOzuPHh7wTrEgBdEMYyfE4F5Pq0FiGdv04kptb587c8aToH345ETJ8dMbXnpOmjanP3nzgJ0iNg8oHIm8oeQODPSP"
    })
    @Disabled("broken upstream as well (TeamNewPipe/NewPipeExtractor dev 6faf4068, 2026-10): the current base.js hides the signature function behind its string table, no known pattern matches it; the visionOS client used for playback serves unciphered or SABR formats")
    @ParameterizedTest
    void testSignatureDeobfuscation(@Nonnull final String videoId,
                                    @Nonnull final String sampleString) throws Exception {
        // As the signature deobfuscation changes frequently with player versions, we can only test
        // that we get a different string than the original one
        assertNotEquals(sampleString,
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, sampleString));
    }
}
