package rachel.materialapps.recordmylive.ui.pages.my;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import rachel.materialapps.recordmylive.logic.entity.FavoriteArtistEntity;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository;

public class FavoriteArtistsViewModel extends AndroidViewModel {
    private final FavoriteArtistRepository repository;
    private final LiveData<List<FavoriteArtistEntity>> artists;
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<FavoriteArtistRepository.Result> result = new MutableLiveData<>();

    public FavoriteArtistsViewModel(@NonNull Application application) {
        super(application);
        repository = new FavoriteArtistRepository(application);
        artists = repository.getAll();
    }

    public LiveData<List<FavoriteArtistEntity>> getArtists() { return artists; }
    public LiveData<Boolean> getBusy() { return busy; }
    public LiveData<FavoriteArtistRepository.Result> getResult() { return result; }
    public void consumeResult() { result.setValue(null); }

    public void add(String name) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        repository.add(name, this::finish);
    }

    public void reorder(List<Long> ids) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        repository.reorder(ids, this::finish);
    }

    public void delete(long id) {
        if (Boolean.TRUE.equals(busy.getValue())) return;
        busy.setValue(true);
        repository.delete(id, this::finish);
    }

    private void finish(FavoriteArtistRepository.Result value) {
        busy.setValue(false);
        result.setValue(value);
    }
}
