package rachel.materialapps.recordmylive.logic.nearby;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;

/** Versioned, bounded BYTES payload. Hashes reduce disclosure but are not secret against guessing. */
public final class ArtistMatchProtocol {
    public static final int MAX_ARTISTS = 1000;
    private static final int MAGIC = 0x47514131;
    private ArtistMatchProtocol() { }

    public static String key(String name) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    ArtistNameNormalizer.comparisonKey(name).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(64);
            for (byte value : hash) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }

    public static byte[] encode(Collection<String> keys) {
        Set<String> unique = new LinkedHashSet<>(keys);
        if (unique.size() > MAX_ARTISTS) throw new IllegalArgumentException("Too many artists");
        ByteBuffer output = ByteBuffer.allocate(8 + unique.size() * 32);
        output.putInt(MAGIC).putInt(unique.size());
        for (String key : unique) {
            if (!key.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("Invalid digest");
            for (int i = 0; i < 64; i += 2) output.put((byte) Integer.parseInt(key.substring(i, i + 2), 16));
        }
        return output.array();
    }

    public static Set<String> decode(byte[] bytes) {
        if (bytes == null || bytes.length < 8 || bytes.length > 8 + MAX_ARTISTS * 32)
            throw new IllegalArgumentException("Invalid size");
        ByteBuffer input = ByteBuffer.wrap(bytes);
        int magic = input.getInt(), count = input.getInt();
        if (magic != MAGIC || count < 0 || count > MAX_ARTISTS || input.remaining() != count * 32)
            throw new IllegalArgumentException("Invalid version or count");
        Set<String> keys = new LinkedHashSet<>();
        for (int n = 0; n < count; n++) {
            StringBuilder key = new StringBuilder(64);
            for (int i = 0; i < 32; i++) key.append(String.format(java.util.Locale.ROOT, "%02x", input.get() & 255));
            keys.add(key.toString());
        }
        return Collections.unmodifiableSet(keys);
    }
}
