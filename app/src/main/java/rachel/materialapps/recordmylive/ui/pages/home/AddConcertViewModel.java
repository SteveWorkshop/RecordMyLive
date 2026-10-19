package rachel.materialapps.recordmylive.ui.pages.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;
import java.util.List;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;

public class AddConcertViewModel extends AndroidViewModel {

    public enum SaveState {
        IDLE, SAVING, SUCCESS, DAILY_LIMIT_REACHED, ERROR
    }

    private final MutableLiveData<SaveState> saveState =
            new MutableLiveData<>(SaveState.IDLE);
    private final ConcertRepository repository;
    private final MutableLiveData<Long> concertId = new MutableLiveData<>();
    private final LiveData<ConcertWithSongs> concertDetail;
    public AddConcertViewModel(@NonNull Application application) {
        super(application);
        repository = new ConcertRepository(application);
        concertDetail = Transformations.switchMap(
                concertId, repositoryId -> repository.getConcertDetail(repositoryId));
    }
    public LiveData<SaveState> getSaveState() {
        return saveState;
    }

    public LiveData<ConcertWithSongs> getConcertDetail() {
        return concertDetail;
    }

    public void loadConcert(long id) {
        concertId.setValue(id);
    }

    public void updateConcert(ConcertEntity concert, List<ConcertSongEntity> songs) {
        if (saveState.getValue() != SaveState.IDLE) {
            return;
        }
        saveState.setValue(SaveState.SAVING);
        repository.updateConcert(concert, songs, updated -> saveState.setValue(
                updated ? SaveState.SUCCESS : SaveState.ERROR));
    }

    public void saveConcert(ConcertEntity concert, List<ConcertSongEntity> songs) {
        if (saveState.getValue() != SaveState.IDLE) {
            return;
        }
        saveState.setValue(SaveState.SAVING);
        repository.saveConcert(concert, songs, result -> {
            switch (result) {
                case SUCCESS:
                    saveState.setValue(SaveState.SUCCESS);
                    break;
                case DAILY_LIMIT_REACHED:
                    saveState.setValue(SaveState.DAILY_LIMIT_REACHED);
                    break;
                case ERROR:
                    saveState.setValue(SaveState.ERROR);
                    break;
            }
        });
    }

    public void resetSaveState() {
        if (saveState.getValue() != SaveState.SAVING) {
            saveState.setValue(SaveState.IDLE);
        }
    }
}
