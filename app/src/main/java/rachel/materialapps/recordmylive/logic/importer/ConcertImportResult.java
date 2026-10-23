package rachel.materialapps.recordmylive.logic.importer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;

/** Platform-neutral draft. Missing information stays absent, never guessed. */
public final class ConcertImportResult {
    public String platform;
    public String sourceUrl;
    public String itemId;
    public String originalName;
    public String name;
    public ConcertType type;
    public String city;
    public String venue;
    public String coverUrl;
    public String localCoverPath;
    public String priceRange;
    public Integer durationMinutes;
    public final List<String> artists = new ArrayList<>();
    public final List<Performance> performances = new ArrayList<>();
    public final List<String> warnings = new ArrayList<>();

    public static final class Performance {
        public String platformId;
        public LocalDate date;
        public LocalTime time;
        public LocalDate saleDate;
        public LocalTime saleTime;

        public String label() {
            return date + (time == null ? "（时间待补充）" : " " + time);
        }
    }
}
