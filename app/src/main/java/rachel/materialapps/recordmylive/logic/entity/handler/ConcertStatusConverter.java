package rachel.materialapps.recordmylive.logic.entity.handler;

import androidx.room.TypeConverter;

import rachel.materialapps.recordmylive.logic.constants.ConcertStatus;

public class ConcertStatusConverter {
    @TypeConverter
    public static Integer toInt(ConcertStatus status) {
        if (status == null) return null;
        return status.ordinal();
    }

    @TypeConverter
    public static ConcertStatus fromInt(Integer value) {
        if (value == null) return null;
        if (value < 0 || value >= ConcertStatus.values().length) {
            return ConcertStatus.PLANNED;
        }
        return ConcertStatus.values()[value];
    }
}
