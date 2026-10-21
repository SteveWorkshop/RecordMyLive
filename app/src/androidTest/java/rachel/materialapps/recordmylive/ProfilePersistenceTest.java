package rachel.materialapps.recordmylive;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository.Result;
import rachel.materialapps.recordmylive.logic.util.ProfileImages;

@RunWith(AndroidJUnit4.class)
public class ProfilePersistenceTest {
    @Test public void failedAvatarReplacementPreservesSavedIdentityAndNewRepositoryReadsIt() throws Exception {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context context = new ContextWrapper(base) {
            @Override public Context getApplicationContext() { return this; }
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("profile-test-" + name, mode);
            }
        };
        SharedPreferences prefs = context.getSharedPreferences("local_profile", Context.MODE_PRIVATE);
        prefs.edit().clear().commit();
        ProfileRepository repo = new ProfileRepository(context);
        File source = File.createTempFile("profile-test-", ".png", context.getCacheDir());
        File draft = null;
        try {
            Bitmap bitmap = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888);
            try (FileOutputStream out = new FileOutputStream(source)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); }
            finally { bitmap.recycle(); }
            draft = await(callback -> repo.prepareImage(Uri.fromFile(source), callback));
            assertNotNull(draft);
            String path = draft.getPath();
            assertEquals(Result.SAVED, this.<Result>await(callback -> repo.save("  Fan　 🎵  ", true, path, callback)));
            String originalAvatar = repo.getAvatarFile();
            File image = ProfileImages.file(context, originalAvatar);
            assertTrue(image.isFile());
            BitmapFactory.Options dimensions = new BitmapFactory.Options();
            dimensions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(image.getPath(), dimensions);
            assertEquals(1024, dimensions.outWidth);
            assertEquals(512, dimensions.outHeight);
            source.delete();
            ProfileRepository reopened = new ProfileRepository(context);
            assertEquals("Fan 🎵", reopened.getNickname());
            assertEquals(originalAvatar, reopened.getAvatarFile());
            assertEquals(Result.ERROR, this.<Result>await(callback -> repo.save("New", true, path + ".missing", callback)));
            assertEquals("Fan 🎵", repo.getNickname());
            assertEquals(originalAvatar, repo.getAvatarFile());
            assertTrue(image.isFile());
            assertEquals(Result.INVALID_NAME, this.<Result>await(callback -> repo.save("　 ", true, path, callback)));
            assertEquals(Result.INVALID_NAME, this.<Result>await(callback -> repo.save("x".repeat(31), false, null, callback)));
            assertEquals(Result.SAVED, this.<Result>await(callback -> repo.save("司 南", false, null, callback)));
            assertEquals(originalAvatar, repo.getAvatarFile());
            assertEquals(Result.SAVED, this.<Result>await(callback -> repo.save("司 南", true, null, callback)));
            assertFalse(image.exists());
            assertNull(repo.getAvatarFile());
        } finally {
            File image = ProfileImages.file(context, repo.getAvatarFile());
            if (image != null) image.delete();
            prefs.edit().clear().commit();
            source.delete();
            if (draft != null) draft.delete();
        }
    }
    private <T> T await(Consumer<Consumer<T>> operation) throws Exception {
        AtomicReference<T> value = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        operation.accept(result -> { value.set(result); done.countDown(); });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        return value.get();
    }
}
