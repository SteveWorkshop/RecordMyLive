package rachel.materialapps.recordmylive;

import static org.junit.Assert.*;
import android.content.Context;
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
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.dao.FavoriteArtistDao;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository.Result;
import rachel.materialapps.recordmylive.logic.util.ArtistImages;

@RunWith(AndroidJUnit4.class)
public class ArtistImagePersistenceTest {
    @Test public void localImageIsResizedAndReplacementFailureRemovalAndDeletionAreSafe() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        FavoriteArtistRepository repo = new FavoriteArtistRepository(context);
        FavoriteArtistDao dao = DatabaseManager.getInstance(context).favoriteArtistDao();
        String name = "Image QA " + System.nanoTime();
        File source = File.createTempFile("artist-test-", ".png", context.getCacheDir());
        File draft = null;
        try {
            Bitmap bitmap = Bitmap.createBitmap(2048, 1024, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(android.graphics.Color.MAGENTA);
            try (FileOutputStream output = new FileOutputStream(source)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            } finally { bitmap.recycle(); }
            draft = await(callback -> repo.prepareImage(Uri.fromFile(source), callback));
            assertNotNull(draft);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(draft.getPath(), options);
            assertEquals(1024, options.outWidth);
            assertEquals(512, options.outHeight);
            String path = draft.getPath();
            assertEquals(Result.ADDED, this.<Result>await(callback -> repo.save(0, name, true, path, callback)));
            FavoriteArtistEntity artist = find(dao, name);
            assertNotNull(artist);
            long id = artist.getId();
            File original = ArtistImages.file(context, artist.getImageFile());
            assertTrue(original.isFile());
            source.delete(); // The provider's original is no longer needed.
            assertEquals(Result.UPDATED, this.<Result>await(callback -> repo.save(id, name + " renamed", false, null, callback)));
            assertEquals(artist.getImageFile(), dao.getById(id).getImageFile());
            assertEquals(Result.ERROR, this.<Result>await(callback -> repo.save(id, name, true, path + ".missing", callback)));
            assertTrue(original.isFile());
            assertEquals(artist.getImageFile(), dao.getById(id).getImageFile());
            assertEquals(Result.DUPLICATE, this.<Result>await(callback -> repo.save(0, name + " renamed", true, path, callback)));
            assertTrue(original.isFile());
            assertEquals(Result.UPDATED, this.<Result>await(callback -> repo.save(id, name, true, path, callback)));
            assertFalse(original.exists());
            File replacement = ArtistImages.file(context, dao.getById(id).getImageFile());
            assertTrue(replacement.isFile());
            assertEquals(Result.UPDATED, this.<Result>await(callback -> repo.save(id, name, true, null, callback)));
            assertNull(dao.getById(id).getImageFile());
            assertFalse(replacement.exists());
            assertEquals(Result.UPDATED, this.<Result>await(callback -> repo.save(id, name, true, path, callback)));
            File last = ArtistImages.file(context, dao.getById(id).getImageFile());
            assertEquals(Result.DELETED, this.<Result>await(callback -> repo.delete(id, callback)));
            assertFalse(last.exists());
        } finally {
            for (FavoriteArtistEntity artist : dao.getAllSync()) {
                if (artist.getName().startsWith(name)) {
                    long id = artist.getId();
                    this.<Result>await(callback -> repo.delete(id, callback));
                }
            }
            source.delete();
            if (draft != null) draft.delete();
        }
    }
    private FavoriteArtistEntity find(FavoriteArtistDao dao, String name) {
        for (FavoriteArtistEntity artist : dao.getAllSync()) if (artist.getName().equals(name)) return artist;
        return null;
    }
    private <T> T await(Consumer<Consumer<T>> operation) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        operation.accept(value -> { result.set(value); done.countDown(); });
        assertTrue("Background operation timed out", done.await(10, TimeUnit.SECONDS));
        return result.get();
    }
}
