package rachel.materialapps.recordmylive.logic.entity;

import androidx.room.ColumnInfo;

import java.io.Serializable;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SeatInfo implements Serializable {
    /**
     * 区
     */
    @ColumnInfo(name = "seat_section")
    private String section;

    /**
     * 排
     */
    @ColumnInfo(name = "seat_row")
    private String row;

    /**
     * 座
     */
    @ColumnInfo(name = "seat_number")
    private String number;
}
