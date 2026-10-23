package rachel.materialapps.recordmylive.logic.importer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Anonymous requests matching the Showstart WAP detail endpoint. */
public final class ShowstartApiClient {
    private static final String BASE_URL = "https://api3.showstart.com";
    private final String device = UUID.randomUUID().toString().replace("-", "");
    private final String visitor = UUID.randomUUID().toString();

    public JsonObject getDetail(String activityId) throws Exception {
        JsonObject tokenResponse = request("/waf/gettoken", new JsonObject(), "");
        if (!"1".equals(string(tokenResponse, "state"))) {
            throw new IOException("秀动暂未提供匿名访问令牌，请稍后重试或手动填写");
        }
        JsonObject accessToken = object(object(tokenResponse, "result"), "accessToken");
        String token = string(accessToken, "access_token");
        if (token.isEmpty()) throw new IOException("秀动访问令牌缺失，请稍后重试或手动填写");

        JsonObject body = new JsonObject();
        body.addProperty("activityId", activityId);
        body.addProperty("coupon", "");
        body.addProperty("shareId", "");
        body.addProperty("previewPwd", "");
        JsonObject response = request("/wap/activity/details", body, token);
        if (!"1".equals(string(response, "state")) || !response.has("result")
                || !response.get("result").isJsonObject()) {
            throw new IOException("秀动暂未返回演出详情，链接可能失效或接口已变更，请稍后重试或手动填写");
        }
        return response.getAsJsonObject("result");
    }

    private JsonObject request(String path, JsonObject body, String accessToken) throws Exception {
        String trace = UUID.randomUUID().toString().replace("-", "") + System.currentTimeMillis();
        body.addProperty("st_flpv", visitor);
        body.addProperty("sign", "");
        body.addProperty("trackPath", "");
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
        String signed = accessToken + "wap" + device + body + path + "997wap" + trace;
        byte[] digest = MessageDigest.getInstance("MD5").digest(signed.getBytes(StandardCharsets.UTF_8));
        StringBuilder signature = new StringBuilder();
        for (byte value : digest) signature.append(String.format("%02x", value & 0xff));

        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("CUSAT", accessToken.isEmpty() ? "nil" : accessToken);
        connection.setRequestProperty("CUSUT", "nil");
        connection.setRequestProperty("CUSIT", "nil");
        connection.setRequestProperty("CUSID", "nil");
        connection.setRequestProperty("CUSNAME", "nil");
        connection.setRequestProperty("CTERMINAL", "wap");
        connection.setRequestProperty("CSAPPID", "wap");
        connection.setRequestProperty("CDEVICENO", device);
        connection.setRequestProperty("CUUSERREF", device);
        connection.setRequestProperty("CVERSION", "997");
        connection.setRequestProperty("CDEVICEINFO", "");
        connection.setRequestProperty("CRTRACEID", trace);
        connection.setRequestProperty("st_flpv", visitor);
        connection.setRequestProperty("CTRACKPATH", "");
        connection.setRequestProperty("CSOURCEPATH", "");
        connection.setRequestProperty("CRPSIGN", signature.toString());
        try {
            try (OutputStream output = connection.getOutputStream()) { output.write(payload); }
            if (connection.getResponseCode() != 200) {
                throw new IOException("秀动服务暂不可用，请稍后重试或手动填写");
            }
            try (InputStream input = connection.getInputStream()) {
                return JsonParser.parseString(new String(DamaiApiClient.readLimited(input, 4 * 1024 * 1024),
                        StandardCharsets.UTF_8)).getAsJsonObject();
            }
        } finally { connection.disconnect(); }
    }

    private static JsonObject object(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonObject()
                ? parent.getAsJsonObject(key) : new JsonObject();
    }

    private static String string(JsonObject parent, String key) {
        return parent.has(key) && parent.get(key).isJsonPrimitive()
                ? parent.get(key).getAsString() : "";
    }
}
