package rachel.materialapps.recordmylive.logic.entity.dto;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.List;

import lombok.Data;
import lombok.ToString;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;

@Data
@ToString
public class ConcertWithSongs {
    @Embedded
    public ConcertEntity concert;

    @Relation(
            parentColumn = "id",
            entityColumn = "concertId"
    )
    public List<ConcertSongEntity> songs;
}
