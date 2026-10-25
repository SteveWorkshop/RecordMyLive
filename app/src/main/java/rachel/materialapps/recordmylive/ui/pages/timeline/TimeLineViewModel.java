package rachel.materialapps.recordmylive.ui.pages.timeline;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;

public class TimeLineViewModel extends AndroidViewModel {
    private final ConcertRepository repository;
    private final LiveData<List<TimeLineItem>> timelineItems;
    private final MutableLiveData<Boolean> initialLoading =
            new MutableLiveData<>(true);

    public TimeLineViewModel(@NonNull Application application) {
        super(application);
        repository = new ConcertRepository(application);
        timelineItems = Transformations.map(
                repository.getAllConcerts(), concerts -> {
                    List<TimeLineItem> items = buildTimelineItems(concerts);
                    initialLoading.setValue(false);
                    return items;
                });
    }

    public LiveData<List<TimeLineItem>> getTimelineItems() {
        return timelineItems;
    }

    public LiveData<Boolean> getInitialLoading() {
        return initialLoading;
    }

    public void deleteConcert(ConcertEntity concert, Consumer<Boolean> callback) {
        repository.deleteConcert(concert, callback);
    }

    /** 数据库已按日期、时间倒序排列；保留顺序，并在每个月首条演出前插入标题。 */
    static List<TimeLineItem> buildTimelineItems(List<ConcertEntity> concerts) {
        if (concerts == null || concerts.isEmpty()) {
            return Collections.emptyList();
        }

        List<TimeLineItem> items = new ArrayList<>();
        YearMonth previousMonth = null;
        for (ConcertEntity concert : concerts) {
            YearMonth month = YearMonth.from(concert.getDate());
            if (!month.equals(previousMonth)) {
                items.add(new TimeLineItem.MonthHeader(month));
                previousMonth = month;
            }
            items.add(new TimeLineItem.ConcertItem(concert));
        }
        return Collections.unmodifiableList(items);
    }
}
