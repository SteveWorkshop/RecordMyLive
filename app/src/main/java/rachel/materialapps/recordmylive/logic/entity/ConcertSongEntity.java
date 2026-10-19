package rachel.materialapps.recordmylive.logic.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
@Entity(
        tableName = "concert_song",
        foreignKeys = @ForeignKey(
                entity = ConcertEntity.class,
                parentColumns = "id",
                childColumns = "concertId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index("concertId")
        }
)
public class ConcertSongEntity implements Serializable {

    /**
     * 主键
     */
    @PrimaryKey(autoGenerate = true)
    private long id;

    /**
     * 所属演出ID
     */
    private long concertId;

    /**
     * 演唱顺序（1、2、3……）
     */
    private int sortOrder;

    /**
     * 歌曲名称
     */
    @NonNull
    private String name;

    /**
     * 是否返场（Encore）
     */
    private boolean encore;

}