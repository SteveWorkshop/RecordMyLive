package rachel.materialapps.recordmylive.logic.entity.handler;

import androidx.room.TypeConverter;

import java.time.LocalTime;

public class LocalTimeConverter {
    @TypeConverter
    public static String fromLocalTime(LocalTime time) {
        return time == null ? null : time.toString();
    }

    @TypeConverter
    public static LocalTime toLocalTime(String value) {
        return value == null ? null : LocalTime.parse(value);
    }

}
