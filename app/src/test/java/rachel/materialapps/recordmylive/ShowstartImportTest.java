package rachel.materialapps.recordmylive;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import org.junit.Test;
import rachel.materialapps.recordmylive.logic.importer.ConcertImportResult;
import rachel.materialapps.recordmylive.logic.importer.ConcertLinkImportService;
import rachel.materialapps.recordmylive.logic.importer.ConcertLinkParser;
import rachel.materialapps.recordmylive.logic.importer.DamaiLinkParser;
import rachel.materialapps.recordmylive.logic.importer.ShowstartLinkParser;

public class ShowstartImportTest {
    private JsonObject detail() {
        return JsonParser.parseString("{\"activityName\":\"【需要人类的参与】- Matt吕彦良x李语庭x秦凡淇\","
                + "\"price\":\"¥168 - 318\",\"avatar\":\"https://s2.showstart.com/cover.jpg\","
                + "\"duration\":\"约150分钟（实际以现场为准）\","
                + "\"showStartTime\":1792841400000,"
                + "\"site\":{\"name\":\"1919 LIVEHOUSE\",\"cityName\":\"北京\"},"
                + "\"sessionUserInfos\":[{\"sessionId\":3539449,\"userInfos\":["
                + "{\"name\":\"Matt吕彦良\"},{\"name\":\"李语庭\"},{\"name\":\"秦凡淇\"}]}]}")
                .getAsJsonObject();
    }

    @Test public void mapsVerifiedSingleSessionWithoutInventingSaleTime() {
        ConcertImportResult result = ShowstartLinkParser.map(detail());
        assertEquals("北京", result.city);
        assertEquals("1919 LIVEHOUSE", result.venue);
        assertEquals(Integer.valueOf(150), result.durationMinutes);
        assertEquals(3, result.artists.size());
        assertEquals(1, result.performances.size());
        assertEquals(LocalDate.of(2026, 10, 24), result.performances.get(0).date);
        assertEquals(LocalTime.of(19, 30), result.performances.get(0).time);
        assertNull(result.performances.get(0).saleDate);
        assertNull(result.performances.get(0).saleTime);
    }

    @Test public void multipleSessionsDoNotUseOverallTimeForEachSession() {
        JsonObject data = detail();
        data.getAsJsonArray("sessionUserInfos").add(data.getAsJsonArray("sessionUserInfos").get(0).deepCopy());
        ConcertImportResult result = ShowstartLinkParser.map(data);
        assertTrue(result.performances.isEmpty());
        assertFalse(result.warnings.isEmpty());
    }

    @Test public void acceptsOnlyRealShowstartHostsAndNumericId() throws Exception {
        ShowstartLinkParser parser = new ShowstartLinkParser();
        URI uri = URI.create("https://wap.showstart.com/pages/activity/detail/detail?activityId=310394");
        assertTrue(parser.supports(uri));
        assertEquals("310394", ShowstartLinkParser.extractActivityId(uri));
        assertFalse(parser.supports(URI.create("https://showstart.com.evil.test/?activityId=310394")));
        assertFalse(parser.supports(URI.create("https://evilshowstart.com/?activityId=310394")));
    }

    @Test public void extractsUrlFromShowstartShareText() throws Exception {
        ConcertLinkParser fake = new ConcertLinkParser() {
            public boolean supports(URI uri) { return new ShowstartLinkParser().supports(uri); }
            public ConcertImportResult parse(URI uri) throws Exception {
                ConcertImportResult result = new ConcertImportResult();
                result.itemId = ShowstartLinkParser.extractActivityId(uri);
                return result;
            }
        };
        ConcertLinkImportService service = new ConcertLinkImportService(
                Arrays.asList(new DamaiLinkParser(), fake));
        String share = "&&`buhwhux,201285-35448287&&【秀动】"
                + "https://wap.showstart.com/pages/activity/detail/detail?activityId=310394"
                + "【【需要人类的参与】- Matt吕彦良x李语庭x秦凡淇】点击链接可直接查看";
        assertEquals("310394", service.parse(share).itemId);
    }
}
