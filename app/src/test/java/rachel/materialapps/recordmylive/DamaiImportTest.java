package rachel.materialapps.recordmylive;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import rachel.materialapps.recordmylive.logic.importer.*;
import static org.junit.Assert.*;

public class DamaiImportTest {
    private JsonObject detail(String showTime, String rules) {
        JsonObject data = JsonParser.parseString("{\"item\":{\"itemName\":\"【南京】测试演出\","
                + "\"showDuration\":\"约120分钟（以现场为准）\"},"
                + "\"venue\":{\"venueProvinceName\":\"江苏省\",\"venueCityName\":\"南京市\"},"
                + "\"price\":{\"range\":\"390-1960\"}}").getAsJsonObject();
        data.getAsJsonObject("item").addProperty("showTime", showTime);
        JsonObject term = new JsonObject();
        term.addProperty("tagDescJson", rules);
        com.google.gson.JsonArray terms = new com.google.gson.JsonArray();
        terms.add(term);
        JsonObject tips = new JsonObject();
        tips.add("serviceTerm", terms);
        data.add("serviceTips", tips);
        return data;
    }

    @Test public void explicitPerformancesRetainDateAndTime() {
        ConcertImportResult result = DamaiLinkParser.map(detail("2026.10.23/10.25",
                "{\"performRules\":[{\"performDate\":\"2026-10-23 周五 19:00\"},"
                        + "{\"performDate\":\"2026-10-25 周日 19:00\"}]}"));
        assertEquals(2, result.performances.size());
        assertEquals(LocalDate.of(2026, 10, 25), result.performances.get(1).date);
        assertEquals(LocalTime.of(19, 0), result.performances.get(1).time);
        assertNull(result.performances.get(0).saleDate);
        assertEquals("测试演出", result.name);
        assertEquals("江苏省 南京市", result.city);
        assertEquals(Integer.valueOf(120), result.durationMinutes);
        assertEquals("390-1960", result.priceRange);
    }

    @Test public void dateRangeAndBrokenOptionalMetadataDoNotInventPerformance() {
        ConcertImportResult result = DamaiLinkParser.map(detail("2026.10.24-11.15", "not json"));
        assertTrue(result.performances.isEmpty());
        assertFalse(result.warnings.isEmpty());
    }

    @Test public void singleDateMayHaveUnknownTime() {
        ConcertImportResult result = DamaiLinkParser.map(detail("2026.10.24", ""));
        assertEquals(1, result.performances.size());
        assertNull(result.performances.get(0).time);
    }

    @Test public void removesOnlyKnownEntryNoticeAtEndOfTitle() {
        JsonObject data = detail("2026.10.24", "");
        data.getAsJsonObject("item").addProperty("itemName",
                "【南京】测试演出（人、票、证信息不匹配无法入场）");
        ConcertImportResult result = DamaiLinkParser.map(data);
        assertEquals("测试演出", result.name);
        assertEquals("【南京】测试演出（人、票、证信息不匹配无法入场）", result.originalName);

        data.getAsJsonObject("item").addProperty("itemName",
                "【南京】测试演出(人,票,证信息不匹配无法入场)  ");
        assertEquals("测试演出", DamaiLinkParser.map(data).name);
    }

    @Test public void retainsOtherParenthesesAndNonSuffixNotices() {
        JsonObject data = detail("2026.10.24", "");
        data.getAsJsonObject("item").addProperty("itemName",
                "【南京】测试演出（加场）（人、票、证信息不匹配无法入场）");
        assertEquals("测试演出（加场）", DamaiLinkParser.map(data).name);

        data.getAsJsonObject("item").addProperty("itemName", "【南京】测试演出（南京站）");
        assertEquals("测试演出（南京站）", DamaiLinkParser.map(data).name);

        data.getAsJsonObject("item").addProperty("itemName",
                "【南京】测试演出（人、票、证信息不匹配无法入场）特别场");
        assertEquals("测试演出（人、票、证信息不匹配无法入场）特别场",
                DamaiLinkParser.map(data).name);
    }

    @Test public void onlyActualDamaiHostsAreSupported() {
        DamaiLinkParser parser = new DamaiLinkParser();
        assertTrue(parser.supports(URI.create("https://m.damai.cn/shows/item.html?itemId=1090372637025")));
        assertFalse(parser.supports(URI.create("https://damai.cn.evil.test/?itemId=1090372637025")));
        assertFalse(parser.supports(URI.create("https://evildamai.cn/?itemId=1090372637025")));
    }

    @Test public void shareTextExtractsLinkWithoutContactingUnrelatedHosts() throws Exception {
        ConcertLinkParser fake = new ConcertLinkParser() {
            public boolean supports(URI uri) { return new DamaiLinkParser().supports(uri); }
            public ConcertImportResult parse(URI uri) throws Exception {
                ConcertImportResult result = new ConcertImportResult();
                result.itemId = DamaiLinkParser.extractItemId(uri);
                return result;
            }
        };
        ConcertLinkImportService service = new ConcertLinkImportService(Collections.singletonList(fake));
        assertEquals("1090372637025", service.parse("分享演出：https://m.damai.cn/shows/item.html?itemId=1090372637025&from=appshare）").itemId);
    }
}
