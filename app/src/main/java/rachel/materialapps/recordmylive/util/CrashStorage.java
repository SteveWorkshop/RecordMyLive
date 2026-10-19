package rachel.materialapps.recordmylive.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class CrashStorage {

    private static final String DIR_NAME = "crash";

    private static final String JSON_NAME = "latest.json";

    private static final String TXT_NAME = "latest.txt";

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private CrashStorage() {

    }

    /**
     * 保存 CrashReport
     */
    public static void save(@NonNull Context context,
                            @NonNull CrashReport report) {

        File dir = getCrashDirectory(context);

        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }

        writeJson(getJsonFile(context), report);

        writeText(getTextFile(context),
                CrashFormatter.format(report));
    }

    /**
     * 读取 CrashReport
     */
    public static CrashReport load(Context context) {

        File file = getJsonFile(context);

        if (!file.exists()) {
            return null;
        }

        try {

            String json = readFile(file);

            return GSON.fromJson(json, CrashReport.class);

        } catch (Exception e) {

            return null;

        }

    }

    /**
     * 读取格式化后的文本
     */
    public static String readText(Context context) {

        File file = getTextFile(context);

        if (!file.exists()) {
            return "";
        }

        try {

            return readFile(file);

        } catch (IOException e) {

            return "";

        }

    }

    /**
     * 是否存在 Crash 日志
     */
    public static boolean exists(Context context) {

        return getJsonFile(context).exists();

    }

    /**
     * 删除 Crash 日志
     */
    public static void delete(Context context) {

        File json = getJsonFile(context);

        File txt = getTextFile(context);

        if (json.exists()) {
            //noinspection ResultOfMethodCallIgnored
            json.delete();
        }

        if (txt.exists()) {
            //noinspection ResultOfMethodCallIgnored
            txt.delete();
        }

    }

    /**
     * Crash 目录
     */
    public static File getCrashDirectory(Context context) {

        return new File(context.getFilesDir(), DIR_NAME);

    }

    /**
     * JSON 文件
     */
    public static File getJsonFile(Context context) {

        return new File(getCrashDirectory(context), JSON_NAME);

    }

    /**
     * TXT 文件
     */
    public static File getTextFile(Context context) {

        return new File(getCrashDirectory(context), TXT_NAME);

    }

    /**
     * 写 JSON
     */
    private static void writeJson(File file,
                                  CrashReport report) {

        writeString(file,
                GSON.toJson(report));

    }

    /**
     * 写文本
     */
    private static void writeText(File file,
                                  String text) {

        writeString(file, text);

    }

    /**
     * 写字符串
     */
    private static void writeString(File file,
                                    String text) {

        try (BufferedWriter writer =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new FileOutputStream(file),
                                     StandardCharsets.UTF_8))) {

            writer.write(text);

            writer.flush();

        } catch (IOException ignored) {

        }

    }

    /**
     * 读文件
     */
    private static String readFile(File file)
            throws IOException {

        StringBuilder builder = new StringBuilder();

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new FileInputStream(file),
                                     StandardCharsets.UTF_8))) {

            String line;

            while ((line = reader.readLine()) != null) {

                builder.append(line)
                        .append('\n');

            }

        }

        return builder.toString();

    }

}