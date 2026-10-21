package rachel.materialapps.recordmylive;

import org.junit.Test;

import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArtistNameNormalizerTest {
    @Test
    public void collapsesWhitespaceAndPreservesEnglishWordBoundaries() {
        assertEquals("Taylor Swift", ArtistNameNormalizer.normalize("  Taylor  \tSwift\n"));
        assertEquals("Adele Live", ArtistNameNormalizer.normalize(" Adele Live\t\n"));
        assertEquals("Taylor Swift", ArtistNameNormalizer.normalize("Taylor\u00a0　Swift"));
    }

    @Test
    public void removesWhitespaceBetweenHanCharacters() {
        assertEquals("周杰倫", ArtistNameNormalizer.normalize("周 杰　倫"));
        assertEquals("司南", ArtistNameNormalizer.normalize("司 南  "));
        assertEquals("𠀀南", ArtistNameNormalizer.normalize("𠀀 南"));
    }

    @Test
    public void preservesSpacesAtMixedScriptBoundaries() {
        assertEquals("陈 Eason Chan", ArtistNameNormalizer.normalize(" 陈  Eason   Chan "));
        assertEquals("五月天 Mayday", ArtistNameNormalizer.normalize("五 月 天　Mayday"));
    }

    @Test
    public void handlesEmptyInputAndIsIdempotent() {
        assertEquals("", ArtistNameNormalizer.normalize(null));
        assertEquals("", ArtistNameNormalizer.normalize(""));
        assertEquals("", ArtistNameNormalizer.normalize(" \t　\u00a0\n"));
        String normalized = ArtistNameNormalizer.normalize(" 司 南  Taylor   Swift ");
        assertEquals(normalized, ArtistNameNormalizer.normalize(normalized));
    }

    @Test
    public void comparesWithoutCaseSensitivity() {
        assertTrue(ArtistNameNormalizer.equivalent("TaylorSwift", "taylorswift"));
        assertTrue(ArtistNameNormalizer.equivalent(" Taylor   Swift ", "taylor swift"));
        assertTrue(ArtistNameNormalizer.equivalent("司 南", "司南"));
        assertFalse(ArtistNameNormalizer.equivalent("Taylor Swift", "TaylorSwift"));
    }
}
