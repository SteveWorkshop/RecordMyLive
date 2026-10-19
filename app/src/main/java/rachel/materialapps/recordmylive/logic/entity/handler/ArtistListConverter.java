package rachel.materialapps.recordmylive.logic.entity.handler;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ArtistListConverter {
    private static final Gson GSON = new Gson();

    @TypeConverter
    public static String fromList(List<String> list) {
        return GSON.toJson(list);
    }

    @TypeConverter
    public static List<String> toList(String json) {
        Type type = new TypeToken<List<String>>() {}.getType();
        return GSON.fromJson(json, type);
    }

}
