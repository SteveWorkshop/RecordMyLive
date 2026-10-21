package rachel.materialapps.recordmylive.ui.pages.my;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import java.io.File;
import rachel.materialapps.recordmylive.logic.repository.ProfileRepository;
import rachel.materialapps.recordmylive.logic.util.ProfileImages;

public class ProfileEditorViewModel extends AndroidViewModel {
    final SavedStateHandle state;
    final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    final MutableLiveData<Integer> imageVersion = new MutableLiveData<>(0);
    final MutableLiveData<Boolean> imageError = new MutableLiveData<>(false);
    final MutableLiveData<ProfileRepository.Result> result = new MutableLiveData<>();
    private final ProfileRepository repository;
    private boolean cleared;

    public ProfileEditorViewModel(@NonNull Application app, SavedStateHandle state) {
        super(app);
        this.state = state;
        repository = new ProfileRepository(app);
        if (!state.contains("nickname")) {
            state.set("nickname", repository.getNickname());
            state.set("avatar", repository.getAvatarFile());
        }
    }

    boolean isBusy() { return Boolean.TRUE.equals(busy.getValue()); }
    File image() {
        if (Boolean.TRUE.equals(state.get("imageChanged"))) {
            String draft = state.get("draft");
            return draft == null ? null : new File(draft);
        }
        return ProfileImages.file(getApplication(), state.get("avatar"));
    }
    void chooseImage(Uri uri) {
        if (isBusy()) return;
        busy.setValue(true);
        repository.prepareImage(uri, file -> {
            busy.setValue(false);
            if (cleared) {
                if (file != null) file.delete();
                clearDraft();
            } else if (file == null) imageError.setValue(true);
            else {
                clearDraft();
                state.set("draft", file.getAbsolutePath());
                state.set("imageChanged", true);
                imageVersion.setValue(imageVersion.getValue() + 1);
            }
        });
    }
    void removeImage() {
        if (isBusy()) return;
        clearDraft();
        state.set("imageChanged", true);
        imageVersion.setValue(imageVersion.getValue() + 1);
    }
    void save() {
        if (isBusy()) return;
        busy.setValue(true);
        repository.save(state.get("nickname"), Boolean.TRUE.equals(state.get("imageChanged")),
                state.get("draft"), value -> {
                    busy.setValue(false);
                    if (cleared || value == ProfileRepository.Result.SAVED) clearDraft();
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
