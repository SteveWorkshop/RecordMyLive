package rachel.materialapps.recordmylive.logic.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class TicketPrice {
    private TicketPrice() {
    }

    public static Long parseYuanToCents(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        try {
            BigDecimal yuan = new BigDecimal(value.trim()).setScale(2, RoundingMode.UNNECESSARY);
            if (yuan.signum() < 0) return null;
            return yuan.movePointRight(2).longValueExact();
        } catch (NumberFormatException | ArithmeticException ignored) {
            return null;
        }
    }

    public static String formatCentsAsYuan(long cents) {
        return String.format(Locale.ROOT, "%d.%02d", cents / 100, Math.abs(cents % 100));
    }
}
