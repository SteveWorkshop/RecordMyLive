package rachel.materialapps.recordmylive.ui.pages.timeline;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

/** 时间线中的月份标题或演出条目，供 Adapter 区分视图类型。 */
public abstract class TimeLineItem {
    private TimeLineItem() {
    }

    public static final class MonthHeader extends TimeLineItem {
        private static final DateTimeFormatter FORMATTER =
                DateTimeFormatter.ofPattern("yyyy年M月");
        private final YearMonth month;

        public MonthHeader(YearMonth month) {
            this.month = Objects.requireNonNull(month);
        }

        public YearMonth getMonth() {
            return month;
        }

        public String getTitle() {
            return month.format(FORMATTER);
        }
    }

    public static final class ConcertItem extends TimeLineItem {
        private final ConcertEntity concert;

        public ConcertItem(ConcertEntity concert) {
            this.concert = Objects.requireNonNull(concert);
        }

        public ConcertEntity getConcert() {
            return concert;
        }
    }
}
