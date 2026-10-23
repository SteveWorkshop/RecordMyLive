package rachel.materialapps.recordmylive.logic.importer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Anonymous MTOP session, isolated from the application's other networking. */
public final class DamaiApiClient {
    private static final String APP_KEY = "12574478";
    private static final String ENDPOINT =
            "https://mtop.damai.cn/h5/mtop.damai.item.detail.getdetail/1.0/";

    public JsonObject getDetail(String itemId) throws Exception {
        JsonObject data = new JsonObject();
        data.addProperty("itemId", itemId);
        String payload = data.toString();
        List<String> cookies = new ArrayList<>();
        request(payload, "", "", cookies);
        String token = null;
        for (String cookie : cookies) {
            if (cookie.startsWith("_m_h5_tk=")) {
                token = cookie.substring("_m_h5_tk=".length()).split("_")[0];
            }
        }
        if (token == null) throw new IOException("无法获取大麦访问令牌，请稍后重试");
        String timestamp = Long.toString(System.currentTimeMillis());
        byte[] digest = MessageDigest.getInstance("MD5").digest(
                (token + "&" + timestamp + "&" + APP_KEY + "&" + payload)
                        .getBytes(StandardCharsets.UTF_8));
        StringBuilder signature = new StringBuilder();
        for (byte b : digest) signature.append(String.format("%02x", b & 0xff));
        JsonObject response = request(payload, timestamp, signature.toString(), cookies);
        if (!response.has("ret") || !response.getAsJsonArray("ret").toString().contains("SUCCESS::")) {
            throw new IOException("大麦暂未返回演出详情，可能需要验证、链接已失效或接口已变更，请稍后重试或手动填写");
        }
        return response.getAsJsonObject("data");
    }

    private JsonObject request(String data, String timestamp, String sign, List<String> cookies)
            throws Exception {
        String query = "?jsv=2.7.5&appKey=" + APP_KEY + "&type=originaljson&t=" + timestamp
                + "&sign=" + sign + "&data=" + URLEncoder.encode(data, "UTF-8");
        HttpURLConnection connection = (HttpURLConnection) new URL(ENDPOINT + query).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Referer", "https://m.damai.cn/");
        if (!cookies.isEmpty()) connection.setRequestProperty("Cookie", String.join("; ", cookies));
        try {
            if (connection.getResponseCode() != 200) throw new IOException("大麦服务暂不可用，请稍后重试");
            for (java.util.Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                if ("Set-Cookie".equalsIgnoreCase(header.getKey())) {
                    for (String value : header.getValue()) cookies.add(value.split(";", 2)[0]);
                }
            }
            try (InputStream input = connection.getInputStream()) {
                return JsonParser.parseString(new String(readLimited(input, 4 * 1024 * 1024),
                        StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } finally { connection.disconnect(); }
    }

    public static byte[] readLimited(InputStream input, int limit) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            if (output.size() + count > limit) throw new IOException("返回内容过大");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }
}
