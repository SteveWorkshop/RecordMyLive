package rachel.materialapps.recordmylive.logic.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.logic.util.ProfileImages;

/** A single local identity. No login, remote identifier, or network access. */
public final class ProfileRepository {
    public enum Result { SAVED, INVALID_NAME, ERROR }
    public static final int MAX_NICKNAME_LENGTH = 30;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Context context;
    private final SharedPreferences preferences;

    public ProfileRepository(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences("local_profile", Context.MODE_PRIVATE);
    }

    public String getNickname() { return preferences.getString("nickname", ""); }
    public String getAvatarFile() { return preferences.getString("avatar", null); }

    public static String normalizeNickname(String input) {
        if (input == null) return "";
        return input.replaceAll("[\\s\\p{Z}]+", " ").trim();
    }

    public static boolean isValidNickname(String name) {
        return !name.isEmpty() && name.codePointCount(0, name.length()) <= MAX_NICKNAME_LENGTH
                && name.codePoints().noneMatch(Character::isISOControl)
                && name.codePoints().anyMatch(cp -> !Character.isWhitespace(cp)
                        && !Character.isSpaceChar(cp) && Character.getType(cp) != Character.FORMAT);
    }

    public void prepareImage(Uri uri, Consumer<File> callback) {
        EXECUTOR.execute(() -> {
            File file;
            try { file = ProfileImages.prepare(context, uri); }
            catch (Exception e) { file = null; }
            File value = file;
            main.post(() -> callback.accept(value));
        });
    }

    public void save(String nickname, boolean imageChanged, String draft, Consumer<Result> callback) {
        EXECUTOR.execute(() -> {
            String name = normalizeNickname(nickname);
            Result result;
            File newImage = null;
            if (!isValidNickname(name)) result = Result.INVALID_NAME;
            else {
                String oldAvatar = getAvatarFile();
                String avatar = oldAvatar;
                try {
                    if (imageChanged) {
                        avatar = null;
                        if (draft != null) {
                            newImage = ProfileImages.file(context, UUID.randomUUID() + ".webp");
                            File directory = newImage.getParentFile();
                            if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException();
                            Files.copy(new File(draft).toPath(), newImage.toPath());
                            avatar = newImage.getName();
                        }
                    }
                    // Commit the pair together; discard the previous image only after success.
                    if (!preferences.edit().putString("nickname", name).putString("avatar", avatar).commit())
                        throw new java.io.IOException();
                    if (imageChanged) {
                        File old = ProfileImages.file(context, oldAvatar);
                        if (old != null) old.delete();
                    }
                    result = Result.SAVED;
                } catch (Exception e) {
                    if (newImage != null) newImage.delete();
                    result = Result.ERROR;
                }
            }
            Result value = result;
            main.post(() -> callback.accept(value));
        });
    }
}
