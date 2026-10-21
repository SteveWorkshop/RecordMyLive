package rachel.materialapps.recordmylive.logic.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import com.bumptech.glide.Glide;
import android.widget.ImageView;
import rachel.materialapps.recordmylive.R;

/** Private local copies with bounded dimensions and no source EXIF metadata. */
public final class ArtistImages {
    private ArtistImages() {}
    public static File file(Context context, String name) {
        if (name == null || !name.matches("[a-zA-Z0-9._-]+")) return null;
        return new File(new File(context.getFilesDir(), "artist_images"), name);
    }
    public static void show(ImageView view, String name) {
        showFile(view, file(view.getContext(), name));
    }
    public static void showFile(ImageView view, File file) {
        Glide.with(view).load(file).diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.NONE)
                .placeholder(R.drawable.baseline_music_note_2_24px)
                .error(R.drawable.baseline_music_note_2_24px).circleCrop().into(view);
    }
    public static File prepare(Context context, Uri uri) throws IOException {
        Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(), uri),
                (decoder, info, source) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int width = info.getSize().getWidth(), height = info.getSize().getHeight();
                    float scale = Math.min(1f, 1024f / Math.max(width, height));
                    decoder.setTargetSize(Math.max(1, Math.round(width * scale)),
                            Math.max(1, Math.round(height * scale)));
                });
        File draft = File.createTempFile("artist-", ".webp", context.getCacheDir());
        boolean success = false;
        try (FileOutputStream output = new FileOutputStream(draft)) {
            if (!bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 88, output)) throw new IOException();
            success = true;
            return draft;
        } finally {
            bitmap.recycle();
            if (!success) draft.delete();
        }
    }
}
