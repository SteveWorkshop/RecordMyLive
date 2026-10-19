package rachel.materialapps.recordmylive.logic.entity.handler;

import androidx.room.TypeConverter;

import java.time.LocalDate;

public class LocalDateConverter {
    @TypeConverter
    public static String fromLocalDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    public static LocalDate toLocalDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}
