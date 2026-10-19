package rachel.materialapps.recordmylive;

import org.junit.Test;

import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ArtistNameNormalizerTest {
    @Test
    public void removesAllWhitespaceAndPreservesCase() {
        assertEquals("AdeleLive", ArtistNameNormalizer.normalize(" Adele Live\t\n"));
        assertEquals("周杰倫", ArtistNameNormalizer.normalize("周 杰　倫"));
    }

    @Test
    public void comparesWithoutCaseSensitivity() {
        assertTrue(ArtistNameNormalizer.equivalent("TaylorSwift", "taylorswift"));
    }
}
