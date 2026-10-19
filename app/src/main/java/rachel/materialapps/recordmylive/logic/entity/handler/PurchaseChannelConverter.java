package rachel.materialapps.recordmylive.logic.entity.handler;

import androidx.room.TypeConverter;

import rachel.materialapps.recordmylive.logic.constants.PurchaseChannel;

public class PurchaseChannelConverter {
    @TypeConverter
    public static Integer toInt(PurchaseChannel channel) {
        if (channel == null) return null;
        return channel.ordinal();
    }

    @TypeConverter
    public static PurchaseChannel fromInt(Integer value) {
        if (value == null) return null;
        if (value < 0 || value >= PurchaseChannel.values().length) {
            return PurchaseChannel.OTHER;
        }
        return PurchaseChannel.values()[value];
    }
}
