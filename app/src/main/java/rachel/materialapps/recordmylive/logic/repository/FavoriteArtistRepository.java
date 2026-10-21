package rachel.materialapps.recordmylive.logic.repository;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.logic.dao.FavoriteArtistDao;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;
import rachel.materialapps.recordmylive.logic.util.ArtistImages;

public class FavoriteArtistRepository {
    public enum Result { ADDED, UPDATED, DUPLICATE, INVALID, DELETED, REORDERED, ERROR }
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final FavoriteArtistDao dao;
    private final Context context;
    public FavoriteArtistRepository(Context context) {
        this.context = context.getApplicationContext();
        dao = DatabaseManager.getInstance(context).favoriteArtistDao();
    }
    public LiveData<List<FavoriteArtistEntity>> getAll() { return dao.getAll(); }
    public void load(long id, Consumer<FavoriteArtistEntity> callback) {
        EXECUTOR.execute(() -> {
            FavoriteArtistEntity artist;
            try { artist = dao.getById(id); } catch (RuntimeException e) { artist = null; }
            FavoriteArtistEntity value = artist;
            mainHandler.post(() -> callback.accept(value));
        });
    }
    public void prepareImage(Uri uri, Consumer<File> callback) {
        EXECUTOR.execute(() -> {
            File image;
            try { image = ArtistImages.prepare(context, uri); }
            catch (Exception | OutOfMemoryError e) { image = null; }
            File value = image;
            mainHandler.post(() -> callback.accept(value));
        });
    }
    public void add(String input, Consumer<Result> callback) {
        save(0, input, false, null, callback);
    }
    /** A null draft with imageChanged=true explicitly removes the image. */
    public void save(long id, String input, boolean imageChanged, String draft, Consumer<Result> callback) {
        EXECUTOR.execute(() -> {
            Result result;
            File newImage = null;
            String name = ArtistNameNormalizer.normalize(input);
            try {
                if (name.isEmpty() || name.length() > 100) {
                    result = Result.INVALID;
                } else {
                    FavoriteArtistEntity artist = id == 0 ? new FavoriteArtistEntity() : dao.getById(id);
                    if (artist == null) throw new IllegalStateException("Missing artist");
                    String oldImage = artist.getImageFile();
                    artist.setName(name);
                    artist.setMatchName(ArtistNameNormalizer.comparisonKey(name));
                    if (id == 0) {
                        artist.setCreatedAt(System.currentTimeMillis());
                        artist.setSortOrder(dao.nextSortOrder());
                    }
                    if (imageChanged) {
                        artist.setImageFile(null);
                        if (draft != null) {
                            newImage = ArtistImages.file(context, UUID.randomUUID() + ".webp");
                            File directory = newImage.getParentFile();
                            if (!directory.isDirectory() && !directory.mkdirs()) throw new java.io.IOException();
                            Files.copy(new File(draft).toPath(), newImage.toPath());
                            artist.setImageFile(newImage.getName());
                        }
                    }
                    boolean saved = id == 0 ? dao.insert(artist) != -1 : dao.update(artist) == 1;
                    result = saved ? (id == 0 ? Result.ADDED : Result.UPDATED) : Result.DUPLICATE;
                    if (saved) {
                        newImage = null;
                        if (imageChanged) deleteImage(oldImage);
                    }
                }
            } catch (Exception e) { result = Result.ERROR; }
            if (newImage != null) newImage.delete();
            Result value = result;
            mainHandler.post(() -> callback.accept(value));
        });
    }
    public void reorder(List<Long> ids, Consumer<Result> callback) {
        List<Long> snapshot = new java.util.ArrayList<>(ids);
        EXECUTOR.execute(() -> {
            Result result;
            try { dao.reorder(snapshot); result = Result.REORDERED; }
            catch (RuntimeException e) { result = Result.ERROR; }
            Result value = result;
            mainHandler.post(() -> callback.accept(value));
        });
    }

    private void deleteImage(String name) {
        File file = ArtistImages.file(context, name);
        if (file != null) file.delete();
    }
    public void delete(long id, Consumer<Result> callback) {
        EXECUTOR.execute(() -> {
            Result result;
            try {
                FavoriteArtistEntity artist = dao.getById(id);
                result = dao.delete(id) == 1 ? Result.DELETED : Result.ERROR;
                if (result == Result.DELETED && artist != null) deleteImage(artist.getImageFile());
            } catch (RuntimeException e) { result = Result.ERROR; }
            Result value = result;
            mainHandler.post(() -> callback.accept(value));
        });
    }
}
