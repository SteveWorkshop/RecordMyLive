package rachel.materialapps.recordmylive.logic.entity.handler;

import androidx.room.TypeConverter;

import rachel.materialapps.recordmylive.logic.constants.ConcertType;

public class ConcertTypeConverter {
    @TypeConverter
    public static int toInt(ConcertType type) {
        return type.ordinal();
    }

    @TypeConverter
    public static ConcertType fromInt(int value) {
        return ConcertType.values()[value];
    }
}
