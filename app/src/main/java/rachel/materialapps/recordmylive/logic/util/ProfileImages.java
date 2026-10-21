package rachel.materialapps.recordmylive.logic.util;

import android.content.Context;
import android.net.Uri;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import java.io.File;
import java.io.IOException;
import rachel.materialapps.recordmylive.R;

public final class ProfileImages {
    private ProfileImages() {}

    public static File file(Context context, String name) {
        if (name == null || !name.matches("[a-zA-Z0-9-]+\\.webp")) return null;
        return new File(new File(context.getFilesDir(), "profile_images"), name);
    }

    public static File prepare(Context context, Uri uri) throws IOException {
        // Share the bounded decode/re-encode pipeline, without retaining source metadata or URI grants.
        return ArtistImages.prepare(context, uri);
    }

    public static void show(ImageView view, File file) {
        Glide.with(view).load(file).diskCacheStrategy(DiskCacheStrategy.NONE)
                .placeholder(R.drawable.baseline_person_24px).error(R.drawable.baseline_person_24px)
                .circleCrop().into(view);
    }
}
