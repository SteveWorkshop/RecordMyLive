package rachel.materialapps.recordmylive.logic.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;

@Dao
public interface FavoriteArtistDao {
    @Query("SELECT * FROM favorite_artist WHERE id = :id")
    FavoriteArtistEntity getById(long id);

    @androidx.room.Update(onConflict = OnConflictStrategy.IGNORE)
    int update(FavoriteArtistEntity artist);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(FavoriteArtistEntity artist);

    @Query("SELECT * FROM favorite_artist ORDER BY sortOrder ASC, createdAt DESC, id DESC")
    LiveData<List<FavoriteArtistEntity>> getAll();

    @Query("SELECT * FROM favorite_artist ORDER BY sortOrder ASC, createdAt DESC, id DESC")
    List<FavoriteArtistEntity> getAllSync();

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM favorite_artist")
    long nextSortOrder();

    @Query("UPDATE favorite_artist SET sortOrder = :position WHERE id = :id")
    void setSortOrder(long id, long position);

    /** Update only ordering, never stale names or images from a UI snapshot. */
    @androidx.room.Transaction
    default void reorder(List<Long> ids) {
        List<FavoriteArtistEntity> current = getAllSync();
        java.util.Set<Long> remaining = new java.util.HashSet<>();
        for (FavoriteArtistEntity artist : current) remaining.add(artist.getId());
        long position = 0;
        for (Long id : ids) {
            if (remaining.remove(id)) setSortOrder(id, position++);
        }
        for (FavoriteArtistEntity artist : current) {
            if (remaining.remove(artist.getId())) setSortOrder(artist.getId(), position++);
        }
    }

    @Query("DELETE FROM favorite_artist WHERE id = :id")
    int delete(long id);
}
