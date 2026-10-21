package rachel.materialapps.recordmylive.logic.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import java.time.LocalDate;
import lombok.Data;

@Data
@Entity(tableName = "expense", foreignKeys = @ForeignKey(entity = ConcertEntity.class,
        parentColumns = "id", childColumns = "concertId", onDelete = ForeignKey.CASCADE),
        indices = @Index("concertId"))
public class ExpenseEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private long concertId;
    @NonNull
    private String category = "OTHER";
    private long amountCents;
    @NonNull
    private LocalDate spentDate = LocalDate.now();
    @NonNull
    private String note = "";
}
