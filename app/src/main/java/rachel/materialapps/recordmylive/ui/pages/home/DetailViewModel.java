package rachel.materialapps.recordmylive.ui.pages.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;

public class DetailViewModel extends AndroidViewModel {
    private final ConcertRepository repository;

    private final LiveData<ConcertWithSongs> detail;

    private final MutableLiveData<Long> concertId = new MutableLiveData<>();

    public DetailViewModel(@NonNull Application application) {
        super(application);
        repository = new ConcertRepository(application);
        detail = Transformations.switchMap(
                concertId,
                repository::getConcertDetail
        );
    }

    public LiveData<ConcertWithSongs> getDetail() {
        return detail;
    }

    public void loadConcert(long id) {
        if (concertId.getValue() == null || concertId.getValue() != id) {
            concertId.setValue(id);
        }
    }
}
