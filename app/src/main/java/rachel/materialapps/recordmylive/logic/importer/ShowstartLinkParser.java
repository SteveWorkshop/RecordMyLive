package rachel.materialapps.recordmylive.logic.importer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.net.URLDecoder;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;

public final class ShowstartLinkParser implements ConcertLinkParser {
    @Override public boolean supports(URI uri) {
        String host = uri.getHost();
        return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && host != null && (host.equalsIgnoreCase("showstart.com")
                || host.toLowerCase(Locale.ROOT).endsWith(".showstart.com"));
    }

    @Override public ConcertImportResult parse(URI uri) throws Exception {
        String id = extractActivityId(uri);
        ConcertImportResult result = map(new ShowstartApiClient().getDetail(id));
        result.itemId = id;
        result.platform = "秀动";
        result.sourceUrl = "https://wap.showstart.com/pages/activity/detail/detail?activityId=" + id;
        return result;
    }

    public static String extractActivityId(URI uri) throws Exception {
        String query = uri.getRawQuery();
        if (query != null) {
            for (String entry : query.split("&")) {
                String[] parts = entry.split("=", 2);
                if (parts.length == 2 && "activityId".equalsIgnoreCase(
                        URLDecoder.decode(parts[0], "UTF-8"))) {
                    String id = URLDecoder.decode(parts[1], "UTF-8");
                    if (id.matches("[0-9]{1,20}")) return id;
                }
            }
        }
        throw new IllegalArgumentException("链接中未找到有效的 activityId，请使用秀动演出详情链接");
    }

    public static ConcertImportResult map(JsonObject detail) {
        ConcertImportResult result = new ConcertImportResult();
        result.originalName = string(detail, "activityName");
        result.name = result.originalName.trim();
        if (result.name.isEmpty()) throw new IllegalArgumentException("未解析到演出名称，请手动填写");
        JsonObject site = object(detail, "site");
        result.city = string(site, "cityName");
        result.venue = string(site, "name");
        result.coverUrl = string(detail, "avatar");
        result.priceRange = string(detail, "price");
        String category = string(detail, "activityTag");
        if (category.contains("音乐节")) result.type = ConcertType.MUSIC_FESTIVAL;
        else if (category.contains("演唱会")) result.type = ConcertType.CONCERT;
        else if (category.contains("见面会")) result.type = ConcertType.FAN_MEETING;
        else if (result.venue.toLowerCase(Locale.ROOT).contains("livehouse"))
            result.type = ConcertType.LIVE_HOUSE;

        Matcher duration = Pattern.compile("(?:约)?(\\d+)\\s*分钟").matcher(string(detail, "duration"));
        if (duration.find()) {
            int minutes = Integer.parseInt(duration.group(1));
            if (minutes > 0 && minutes < 6000) result.durationMinutes = minutes;
        }

        JsonArray sessions = array(detail, "sessionUserInfos");
        if (sessions.size() == 1 && sessions.get(0).isJsonObject()) {
            JsonObject session = sessions.get(0).getAsJsonObject();
            result.artists.addAll(artists(array(session, "userInfos")));
            long start = number(detail, "showStartTime");
            if (start > 0) {
                java.time.ZonedDateTime show = Instant.ofEpochMilli(start)
                        .atZone(ZoneId.of("Asia/Shanghai"));
                ConcertImportResult.Performance performance = new ConcertImportResult.Performance();
                performance.platformId = string(session, "sessionId");
                performance.date = show.toLocalDate();
                performance.time = show.toLocalTime();
                result.performances.add(performance);
            }
        } else {
            result.warnings.add("场次时间未能明确对应，请手动填写演出日期和时间");
        }
        if (result.artists.isEmpty()) result.artists.addAll(artists(array(detail, "userInfos")));
        if (result.performances.isEmpty() && sessions.size() == 1) {
            result.warnings.add("未取得明确演出时间，请手动填写");
        }
        result.warnings.add("未取得明确开票时间，请按需手动填写");
        return result;
    }

    private static java.util.List<String> artists(JsonArray users) {
        java.util.List<String> names = new java.util.ArrayList<>();
        for (JsonElement user : users) {
            if (!user.isJsonObject()) continue;
            String name = string(user.getAsJsonObject(), "name").trim();
            if (!name.isEmpty() && !names.contains(name)) names.add(name);
        }
        return names;
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static JsonArray array(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonArray()
                ? parent.getAsJsonArray(key) : new JsonArray();
    }

    private static String string(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonPrimitive()
                ? parent.get(key).getAsString() : "";
    }

    private static long number(JsonObject parent, String key) {
        try { return parent.get(key).getAsLong(); } catch (RuntimeException ignored) { return 0; }
    }
}
