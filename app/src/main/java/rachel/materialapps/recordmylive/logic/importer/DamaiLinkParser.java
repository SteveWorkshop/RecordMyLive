package rachel.materialapps.recordmylive.logic.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;

public final class DamaiLinkParser implements ConcertLinkParser {
    private static final Pattern ENTRY_NOTICE_SUFFIX = Pattern.compile(
            "[（(]\\s*人[、,，\\s]*票[、,，\\s]*证信息不匹配无法入场\\s*[）)]\\s*$");

    @Override public boolean supports(URI uri) {
        String host = uri.getHost();
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && host != null && (host.equalsIgnoreCase("damai.cn")
                || host.toLowerCase(java.util.Locale.ROOT).endsWith(".damai.cn"));
    }

    @Override public ConcertImportResult parse(URI uri) throws Exception {
        String id = extractItemId(uri);
        ConcertImportResult result = map(new DamaiApiClient().getDetail(id));
        result.itemId = id;
        result.platform = "大麦";
        result.sourceUrl = "https://m.damai.cn/shows/item.html?itemId=" + id;
        return result;
    }

    public static String extractItemId(URI uri) throws Exception {
        String query = uri.getRawQuery();
        if (query != null) {
            for (String entry : query.split("&")) {
                String[] parts = entry.split("=", 2);
                String key = URLDecoder.decode(parts[0], "UTF-8");
                if ((key.equalsIgnoreCase("itemId") || key.equals("id")) && parts.length == 2) {
                    String value = URLDecoder.decode(parts[1], "UTF-8");
                    if (value.matches("[0-9]{8,20}")) return value;
                }
            }
        }
        throw new IllegalArgumentException("链接中未找到有效的演出 itemId，请使用大麦演出详情链接");
    }

    public static ConcertImportResult map(JsonObject data) {
        ConcertImportResult result = new ConcertImportResult();
        JsonObject item = object(data, "item");
        result.originalName = string(item, "itemName");
        String title = result.originalName.replaceFirst("^【[^】]+】", "").trim();
        result.name = ENTRY_NOTICE_SUFFIX.matcher(title).replaceFirst("").trim();
        if (result.name.isEmpty()) throw new IllegalArgumentException("未解析到演出名称，请手动填写");
        JsonObject venue = object(data, "venue");
        result.city = (string(venue, "venueProvinceName") + " " + string(venue, "venueCityName")).trim();
        if (result.city.isEmpty()) result.city = string(item, "cityName");
        result.venue = string(venue, "venueName");
        result.priceRange = string(object(data, "price"), "range");
        JsonObject guide = object(data, "guide");
        String category = string(guide, "guideCat");
        if (category.contains("音乐节")) result.type = ConcertType.MUSIC_FESTIVAL;
        else if (category.contains("LiveHouse") || category.contains("livehouse")) result.type = ConcertType.LIVE_HOUSE;
        else if (category.contains("演唱会")) result.type = ConcertType.CONCERT;
        else if (category.contains("见面会")) result.type = ConcertType.FAN_MEETING;
        for (JsonElement artist : array(guide, "artists")) {
            String name = string(artist.getAsJsonObject(), "name").trim();
            if (!name.isEmpty() && !result.artists.contains(name)) result.artists.add(name);
        }
        JsonArray pictures = array(object(item, "itemPics"), "itemPicList");
        if (pictures.size() > 0) result.coverUrl = string(pictures.get(0).getAsJsonObject(), "picUrl");
        Matcher duration = Pattern.compile("(?:约)?(\\d+)\\s*分钟").matcher(string(item, "showDuration"));
        if (duration.find()) {
            int minutes = Integer.parseInt(duration.group(1));
            if (minutes > 0 && minutes < 6000) result.durationMinutes = minutes;
        }
        // Refund metadata contains explicit per-performance dates. Never infer dates from a range.
        for (JsonElement term : array(object(data, "serviceTips"), "serviceTerm")) {
            String raw = string(term.getAsJsonObject(), "tagDescJson");
            if (raw.isEmpty()) continue;
            try {
                JsonObject refund = com.google.gson.JsonParser.parseString(raw).getAsJsonObject();
                for (JsonElement rule : array(refund, "performRules")) {
                    Matcher date = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})(?:.*?(\\d{2}:\\d{2}))?")
                            .matcher(string(rule.getAsJsonObject(), "performDate"));
                    if (!date.find()) continue;
                    ConcertImportResult.Performance performance = new ConcertImportResult.Performance();
                    performance.date = LocalDate.parse(date.group(1));
                    if (date.group(2) != null) performance.time = LocalTime.parse(date.group(2));
                    boolean duplicate = result.performances.stream().anyMatch(p ->
                            p.date.equals(performance.date) && java.util.Objects.equals(p.time, performance.time));
                    if (!duplicate) result.performances.add(performance);
                }
            } catch (RuntimeException ignored) { /* Optional metadata must not prevent import. */ }
        }
        if (result.performances.isEmpty()) {
            String showTime = string(item, "showTime");
            if (showTime.matches("\\d{4}[.-]\\d{2}[.-]\\d{2}")) {
                ConcertImportResult.Performance performance = new ConcertImportResult.Performance();
                performance.date = LocalDate.parse(showTime.replace('.', '-'));
                result.performances.add(performance);
            } else result.warnings.add("未取得明确场次，请手动填写演出日期和时间");
        }
        JsonArray infos = array(item, "performInfos");
        JsonArray limits = array(data, "performLimits");
        // Only attach a sale timestamp when the item unambiguously has one performance.
        if (result.performances.size() == 1 && infos.size() == 1 && limits.size() == 1
                && string(infos.get(0).getAsJsonObject(), "performId")
                .equals(string(limits.get(0).getAsJsonObject(), "performId"))) {
            JsonObject info = infos.get(0).getAsJsonObject();
            ConcertImportResult.Performance performance = result.performances.get(0);
            performance.platformId = string(info, "performId");
            try {
                long timestamp = Long.parseLong(string(info, "saleTime"));
                if (timestamp > 0) {
                    java.time.ZonedDateTime sale = Instant.ofEpochMilli(timestamp).atZone(ZoneId.of("Asia/Shanghai"));
                    performance.saleDate = sale.toLocalDate();
                    performance.saleTime = sale.toLocalTime();
                }
            } catch (RuntimeException ignored) { }
        }
        if (result.performances.stream().noneMatch(p -> p.saleDate != null)) {
            result.warnings.add("开票时间未能明确对应场次，请按需手动填写");
        }
        return result;
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent == null ? null : parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : new JsonObject();
    }
    private static JsonArray array(JsonObject parent, String key) {
        JsonElement value = parent == null ? null : parent.get(key);
        return value != null && value.isJsonArray() ? value.getAsJsonArray() : new JsonArray();
    }
    private static String string(JsonObject parent, String key) {
        JsonElement value = parent == null ? null : parent.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : "";
    }
}
