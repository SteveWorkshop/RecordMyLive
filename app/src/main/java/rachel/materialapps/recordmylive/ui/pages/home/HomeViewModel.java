package rachel.materialapps.recordmylive.ui.pages.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;

public class HomeViewModel extends AndroidViewModel {

    private final ConcertRepository repository;
    private final MutableLiveData<LocalDate> selectedDate =
            new MutableLiveData<>(LocalDate.now());

    private final MutableLiveData<Set<LocalDate>> concertDates =
            new MutableLiveData<>(new HashSet<>());

    private final MutableLiveData<YearMonth> currentMonth = new MutableLiveData<>();

    private final MediatorLiveData<List<ConcertEntity>> concerts =
            new MediatorLiveData<>();
    private final MutableLiveData<Boolean> initialLoading =
            new MutableLiveData<>(true);

    private LiveData<List<ConcertEntity>> currentSource;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository=new ConcertRepository(application);
        selectDate(LocalDate.now());
    }



    public LiveData<LocalDate> getSelectedDate() {
        return selectedDate;
    }

    public LiveData<Set<LocalDate>> getConcertDates() {
        return concertDates;
    }

    public void selectDate(LocalDate date) {
        selectedDate.setValue(date);
        if (currentSource != null) {
            concerts.removeSource(currentSource);
        }
        currentSource = repository.getConcertsByDate(date);
        concerts.addSource(currentSource, items -> {
            concerts.setValue(items);
            if (Boolean.TRUE.equals(initialLoading.getValue())) {
                initialLoading.setValue(false);
            }
        });
    }

    public LiveData<List<ConcertEntity>> getConcerts() {
        return concerts;
    }

    public LiveData<Boolean> getInitialLoading() {
        return initialLoading;
    }

    public LiveData<List<ConcertEntity>> getUpcomingConcerts() {
        return repository.getUpcoming(LocalDate.now());
    }

    public LiveData<Set<LocalDate>> getMonthMarks(){
        return Transformations.switchMap(currentMonth, month ->{
            LocalDate start = month.atDay(1);
            LocalDate end = month.atEndOfMonth();

            return Transformations.map(
                    repository.getByMonth(start, end),
                    list -> {
                        Set<LocalDate> set = new HashSet<>();
                        for (ConcertEntity c : list) {
                            set.add(c.getDate());
                        }
                        return set;
                    }
            );
        });
    }

    public void updateConcertDates(Set<LocalDate> dates) {
        concertDates.setValue(dates);
    }

    public void setMonth(YearMonth ym) {
        currentMonth.setValue(ym);
    }

    public void deleteConcert(ConcertEntity concert, Consumer<Boolean> callback) {
        repository.deleteConcert(concert, callback);
    }
}
