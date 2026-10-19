package rachel.materialapps.recordmylive.ui.pages.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import java.time.LocalDate;

import rachel.materialapps.recordmylive.logic.constants.AppConstants;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;

public class AddConcertViewModel extends AndroidViewModel {

    private final ConcertRepository repository;
    public AddConcertViewModel(@NonNull Application application) {
        super(application);
        repository = new ConcertRepository(application);
    }
    // TODO: Implement the ViewModel

    public void saveConcert(ConcertEntity concert){
        repository.insertConcert(concert);
    }


    public boolean canAddConcert(LocalDate date) {
        return repository.countByDate(date) < AppConstants.MAX_CONCERT_PER_DAY;
    }
}