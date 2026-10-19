package rachel.materialapps.recordmylive.logic.util;

import java.util.Locale;

public final class ArtistNameNormalizer {
    private ArtistNameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder result = new StringBuilder(name.length());
        for (int i = 0; i < name.length();) {
            int codePoint = name.codePointAt(i);
            if (!Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint)) {
                result.appendCodePoint(codePoint);
            }
            i += Character.charCount(codePoint);
        }
        return result.toString();
    }

    public static String comparisonKey(String name) {
        return normalize(name).toLowerCase(Locale.ROOT);
    }

    public static boolean equivalent(String first, String second) {
        return comparisonKey(first).equals(comparisonKey(second));
    }
}
