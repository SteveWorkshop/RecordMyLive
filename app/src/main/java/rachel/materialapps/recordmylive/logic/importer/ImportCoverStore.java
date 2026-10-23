package rachel.materialapps.recordmylive.logic.importer;

import android.content.Context;
import android.graphics.BitmapFactory;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

/** Stores covers in private persistent storage; no media/storage permission is needed. */
public final class ImportCoverStore {
    public static String download(Context context, String address) throws Exception {
        if (address == null || address.isEmpty()) return null;
        URL url = new URL(address.startsWith("//") ? "https:" + address : address);
        String host = url.getHost().toLowerCase(java.util.Locale.ROOT);
        if (!"https".equals(url.getProtocol()) || !(host.endsWith(".alicdn.com")
                || host.equals("showstart.com") || host.endsWith(".showstart.com"))) return null;
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(15000);
        connection.setInstanceFollowRedirects(false);
        try {
            if (connection.getResponseCode() != 200) return null;
            byte[] bytes;
            try (InputStream input = connection.getInputStream()) {
                bytes = DamaiApiClient.readLimited(input, 8 * 1024 * 1024);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            if (options.outWidth <= 0 || options.outHeight <= 0
                    || (long) options.outWidth * options.outHeight > 32000000) return null;
            File directory = new File(context.getFilesDir(), "concert_covers");
            if (!directory.isDirectory() && !directory.mkdirs()) return null;
            File file = new File(directory, UUID.randomUUID() + ".img");
            try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); }
            return file.getAbsolutePath();
        } finally { connection.disconnect(); }
    }
}
