package rachel.materialapps.recordmylive.logic.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import lombok.Data;

/** User-selected favorites, independent of concert history. IDs are local only. */
@Data
@Entity(tableName = "favorite_artist", indices = @Index(value = "matchName", unique = true))
public class FavoriteArtistEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @NonNull
    private String name = "";
    @NonNull
    private String matchName = "";
    private long createdAt;
    @androidx.room.ColumnInfo(defaultValue = "0")
    private long sortOrder;
    @androidx.room.ColumnInfo(defaultValue = "0")
    private boolean nearbyMatchSelected;
    /** File name in the app-private artist_images directory; never a remote URL. */
    private String imageFile;
}
