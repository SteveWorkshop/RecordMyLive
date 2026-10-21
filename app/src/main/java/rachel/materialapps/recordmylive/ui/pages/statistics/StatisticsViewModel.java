package rachel.materialapps.recordmylive.ui.pages.statistics;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rachel.materialapps.recordmylive.logic.constants.ConcertStatus;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;
import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.repository.ExpenseRepository;
import rachel.materialapps.recordmylive.logic.util.ExpenseTotals;

public class StatisticsViewModel extends AndroidViewModel {

    public enum Period { MONTH, YEAR, ALL }

    public static class RankItem {
        public final String name;
        public final int count;

        RankItem(String name, int count) {
            this.name = name;
            this.count = count;
        }
    }

    public static class Summary {
        public final int concertCount;
        public final int cityCount;
        public final long totalDurationMinutes;
        public final long totalCostCents;
        public final ExpenseTotals expenses;
        public final List<RankItem> topArtists;
        public final List<RankItem> topCities;
        public final List<RankItem> topTypes;

        Summary(int concertCount, int cityCount, long totalDurationMinutes, ExpenseTotals expenses,
                List<RankItem> topArtists, List<RankItem> topCities,
                List<RankItem> topTypes) {
            this.concertCount = concertCount;
            this.cityCount = cityCount;
            this.totalDurationMinutes = totalDurationMinutes;
            this.expenses = expenses;
            this.totalCostCents = expenses.totalCents;
            this.topArtists = topArtists;
            this.topCities = topCities;
            this.topTypes = topTypes;
        }
    }

    private final MutableLiveData<Period> period = new MutableLiveData<>(Period.MONTH);
    private final MutableLiveData<YearMonth> anchor = new MutableLiveData<>(YearMonth.now());
    private final MediatorLiveData<Summary> summary = new MediatorLiveData<>();
    private List<ConcertEntity> concerts = Collections.emptyList();
    private List<ExpenseEntity> expenses = Collections.emptyList();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        ConcertRepository repository = new ConcertRepository(application);
        summary.addSource(repository.getAllConcerts(), items -> {
            concerts = items == null ? Collections.emptyList() : items;
            recalculate();
        });
        summary.addSource(period, ignored -> recalculate());
        summary.addSource(new ExpenseRepository(application).getAll(), items -> {
            expenses = items == null ? Collections.emptyList() : items;
            recalculate();
        });
        summary.addSource(anchor, ignored -> recalculate());
    }

    public LiveData<Period> getPeriod() { return period; }

    public LiveData<YearMonth> getAnchor() { return anchor; }

    public LiveData<Summary> getSummary() { return summary; }

    public void setPeriod(Period value) {
        if (value != period.getValue()) period.setValue(value);
    }

    public void movePeriod(int offset) {
        YearMonth value = anchor.getValue();
        Period selected = period.getValue();
        if (value == null || selected == null || selected == Period.ALL) return;
        anchor.setValue(selected == Period.MONTH
                ? value.plusMonths(offset) : value.plusYears(offset));
    }

    private void recalculate() {
        Period selected = period.getValue();
        YearMonth selectedAnchor = anchor.getValue();
        if (selected == null || selectedAnchor == null) return;

        int count = 0;
        long duration = 0;
        List<ConcertEntity> selectedConcerts = new ArrayList<>();
        Map<String, Integer> artists = new HashMap<>();
        Map<String, String> artistDisplayNames = new HashMap<>();
        Map<String, Integer> cities = new HashMap<>();
        Map<String, Integer> types = new HashMap<>();

        for (ConcertEntity concert : concerts) {
            if (concert.getStatus() != ConcertStatus.ATTENDED
                    || !isInPeriod(concert, selected, selectedAnchor)) continue;
            count++;
            selectedConcerts.add(concert);
            if (concert.getDurationMinutes() != null && concert.getDurationMinutes() > 0) {
                duration += concert.getDurationMinutes();
            }
            String city = clean(concert.getCity());
            if (!city.isEmpty()) cities.merge(city, 1, Integer::sum);
            if (concert.getType() != null) {
                types.merge(concert.getType().getDisplayName(), 1, Integer::sum);
            }

            Set<String> uniqueArtists = new HashSet<>();
            if (concert.getArtists() != null) {
                for (String artist : concert.getArtists()) {
                    String normalized = ArtistNameNormalizer.normalize(artist);
                    if (!normalized.isEmpty()) {
                        String key = ArtistNameNormalizer.comparisonKey(normalized);
                        uniqueArtists.add(key);
                        artistDisplayNames.putIfAbsent(key, normalized);
                    }
                }
            }
            for (String artist : uniqueArtists) artists.merge(artist, 1, Integer::sum);
        }
        summary.setValue(new Summary(count, cities.size(), duration,
                ExpenseTotals.calculate(selectedConcerts, expenses),
                topThree(artists, artistDisplayNames), topThree(cities, null),
                topThree(types, null)));
    }

    private boolean isInPeriod(ConcertEntity concert, Period selected, YearMonth selectedAnchor) {
        if (selected == Period.ALL) return true;
        if (concert.getDate() == null) return false;
        if (selected == Period.YEAR) return concert.getDate().getYear() == selectedAnchor.getYear();
        return YearMonth.from(concert.getDate()).equals(selectedAnchor);
    }

    private List<RankItem> topThree(Map<String, Integer> values,
                                    Map<String, String> displayNames) {
        List<RankItem> items = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : values.entrySet()) {
            String name = displayNames == null
                    ? entry.getKey() : displayNames.getOrDefault(entry.getKey(), entry.getKey());
            items.add(new RankItem(name, entry.getValue()));
        }
        items.sort(Comparator.comparingInt((RankItem item) -> item.count).reversed()
                .thenComparing(item -> item.name));
        return new ArrayList<>(items.subList(0, Math.min(3, items.size())));
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
