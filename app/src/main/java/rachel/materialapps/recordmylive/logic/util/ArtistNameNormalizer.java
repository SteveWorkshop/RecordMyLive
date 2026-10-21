package rachel.materialapps.recordmylive.logic.util;

import java.util.Locale;

public final class ArtistNameNormalizer {
    private ArtistNameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null || name.isEmpty()) return "";
        StringBuilder result = new StringBuilder(name.length());
        boolean pendingSpace = false;
        int previousCodePoint = -1;
        for (int i = 0; i < name.length();) {
            int codePoint = name.codePointAt(i);
            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                pendingSpace = result.length() > 0;
            } else {
                if (pendingSpace && !(isHan(previousCodePoint) && isHan(codePoint))) {
                    result.append(' ');
                }
                result.appendCodePoint(codePoint);
                previousCodePoint = codePoint;
                pendingSpace = false;
            }
            i += Character.charCount(codePoint);
        }
        return result.toString();
    }

    private static boolean isHan(int codePoint) {
        return codePoint >= 0 && Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    public static String comparisonKey(String name) {
        return normalize(name).toLowerCase(Locale.ROOT);
    }

    public static boolean equivalent(String first, String second) {
        return comparisonKey(first).equals(comparisonKey(second));
    }
}
