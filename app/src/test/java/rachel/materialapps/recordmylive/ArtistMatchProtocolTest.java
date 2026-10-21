package rachel.materialapps.recordmylive;

import org.junit.Test;
import java.util.*;
import rachel.materialapps.recordmylive.logic.nearby.ArtistMatchProtocol;
import static org.junit.Assert.*;

public class ArtistMatchProtocolTest {
    @Test public void normalizedNamesMatchWithoutSendingNamesOrImages() {
        String chinese = ArtistMatchProtocol.key("司 南  ");
        String english = ArtistMatchProtocol.key(" Taylor   Swift ");
        assertEquals(chinese, ArtistMatchProtocol.key("司南"));
        assertEquals(english, ArtistMatchProtocol.key("taylor swift"));
        assertNotEquals(english, ArtistMatchProtocol.key("TaylorSwift"));
        Set<String> expected = new HashSet<>(Arrays.asList(chinese, english));
        byte[] payload = ArtistMatchProtocol.encode(Arrays.asList(chinese, english, chinese));
        assertEquals(72, payload.length);
        assertEquals(expected, ArtistMatchProtocol.decode(payload));
        assertTrue(ArtistMatchProtocol.decode(ArtistMatchProtocol.encode(Collections.emptyList())).isEmpty());
    }

    @Test public void rejectsWrongVersionTruncationTrailingBytesAndOversizedMessages() {
        byte[] valid = ArtistMatchProtocol.encode(Collections.singleton(ArtistMatchProtocol.key("司南")));
        byte[] wrongVersion = valid.clone(); wrongVersion[3]++;
        for (byte[] invalid : Arrays.asList(new byte[0], wrongVersion, Arrays.copyOf(valid, valid.length - 1),
                Arrays.copyOf(valid, valid.length + 1), new byte[32009])) {
            assertThrows(IllegalArgumentException.class, () -> ArtistMatchProtocol.decode(invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> ArtistMatchProtocol.decode(null));
        Set<String> many = new HashSet<>();
        for (int i = 0; i < 1001; i++) many.add(ArtistMatchProtocol.key("artist" + i));
        assertThrows(IllegalArgumentException.class, () -> ArtistMatchProtocol.encode(many));
    }
}
