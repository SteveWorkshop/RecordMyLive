package rachel.materialapps.recordmylive.ui.pages.my;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import java.io.File;
import rachel.materialapps.recordmylive.logic.repository.FavoriteArtistRepository;

public class ArtistEditorViewModel extends AndroidViewModel {
    final SavedStateHandle state;
    final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    final MutableLiveData<Boolean> ready = new MutableLiveData<>(false);
    final MutableLiveData<Integer> imageVersion = new MutableLiveData<>(0);
    final MutableLiveData<FavoriteArtistRepository.Result> result = new MutableLiveData<>();
    final MutableLiveData<Boolean> imageError = new MutableLiveData<>(false);
    private final FavoriteArtistRepository repository;
    private final long id;
    private boolean cleared;

    public ArtistEditorViewModel(@NonNull Application app, SavedStateHandle state) {
        super(app);
        this.state = state;
        Long argument = state.get("artistId");
        id = argument == null ? 0 : argument;
        repository = new FavoriteArtistRepository(app);
        if (state.contains("name")) ready.setValue(true);
        else if (id == 0) {
            state.set("name", "");
            ready.setValue(true);
        } else {
            busy.setValue(true);
            repository.load(id, artist -> {
                busy.setValue(false);
                if (artist == null) result.setValue(FavoriteArtistRepository.Result.ERROR);
                else {
                    state.set("name", artist.getName());
                    state.set("imageFile", artist.getImageFile());
                    ready.setValue(true);
                    refreshImage();
                }
            });
        }
    }

    boolean isEditing() { return id != 0; }
    boolean isBusy() { return Boolean.TRUE.equals(busy.getValue()); }
    boolean imageChanged() { return Boolean.TRUE.equals(state.get("imageChanged")); }
    File image() {
        if (imageChanged()) {
            String draft = state.get("draft");
            return draft == null ? null : new File(draft);
        }
        return rachel.materialapps.recordmylive.logic.util.ArtistImages.file(getApplication(), state.get("imageFile"));
    }
    private void refreshImage() { imageVersion.setValue(imageVersion.getValue() + 1); }
    void chooseImage(Uri uri) {
        if (isBusy() || !Boolean.TRUE.equals(ready.getValue())) return;
        busy.setValue(true);
        repository.prepareImage(uri, file -> {
            busy.setValue(false);
            if (cleared) {
                if (file != null) file.delete();
                clearDraft();
                return;
            }
            if (file == null) imageError.setValue(true);
            else {
                clearDraft();
                state.set("draft", file.getAbsolutePath());
                state.set("imageChanged", true);
                refreshImage();
            }
        });
    }
    void removeImage() {
        if (isBusy()) return;
        clearDraft();
        state.set("imageChanged", true);
        refreshImage();
    }
    void save() {
        if (isBusy() || !Boolean.TRUE.equals(ready.getValue())) return;
        busy.setValue(true);
        repository.save(id, state.get("name"), imageChanged(), state.get("draft"), value -> {
            busy.setValue(false);
            if (cleared || value == FavoriteArtistRepository.Result.ADDED
                    || value == FavoriteArtistRepository.Result.UPDATED) clearDraft();
            result.setValue(value);
        });
    }
    private void clearDraft() {
        String draft = state.get("draft");
        if (draft != null) new File(draft).delete();
        state.set("draft", null);
    }
    @Override protected void onCleared() {
        cleared = true;
        if (!isBusy()) clearDraft();
    }
}
