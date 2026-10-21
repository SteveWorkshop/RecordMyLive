package rachel.materialapps.recordmylive.ui.pages.my;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentProfileEditorBinding;
import rachel.materialapps.recordmylive.logic.util.ProfileImages;

public class ProfileEditorFragment extends Fragment {
    private FragmentProfileEditorBinding binding;
    private ProfileEditorViewModel model;
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) model.chooseImage(uri);
            });

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                                       @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileEditorBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(this).get(ProfileEditorViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_save) return false;
            model.save();
            return true;
        });
        binding.nicknameInput.setText(model.state.get("nickname"));
        binding.nicknameInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.state.set("nickname", s.toString());
                binding.nicknameLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        binding.nicknameInput.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) return false;
            model.save();
            return true;
        });
        binding.chooseAvatar.setOnClickListener(v -> picker.launch(new String[]{"image/*"}));
        binding.removeAvatar.setOnClickListener(v -> model.removeImage());
        model.busy.observe(getViewLifecycleOwner(), busy -> {
            boolean enabled = !Boolean.TRUE.equals(busy);
            binding.nicknameInput.setEnabled(enabled);
            binding.chooseAvatar.setEnabled(enabled);
            binding.removeAvatar.setEnabled(enabled);
            binding.toolbar.getMenu().findItem(R.id.action_save).setEnabled(enabled);
            binding.progress.setVisibility(enabled ? View.GONE : View.VISIBLE);
        });
        model.imageVersion.observe(getViewLifecycleOwner(), version -> {
            ProfileImages.show(binding.profileAvatar, model.image());
            boolean hasImage = model.image() != null;
            binding.removeAvatar.setVisibility(hasImage ? View.VISIBLE : View.GONE);
            binding.chooseAvatar.setText(hasImage ? R.string.profile_change_avatar : R.string.profile_choose_avatar);
        });
        model.imageError.observe(getViewLifecycleOwner(), error -> {
            if (Boolean.TRUE.equals(error)) {
                model.imageError.setValue(false);
                Toast.makeText(requireContext(), R.string.profile_image_error, Toast.LENGTH_SHORT).show();
            }
        });
        model.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            model.result.setValue(null);
            switch (result) {
                case SAVED:
                    NavHostFragment.findNavController(this).navigateUp();
                    break;
                case INVALID_NAME:
                    binding.nicknameLayout.setError(getString(R.string.profile_nickname_invalid));
                    break;
                default:
                    Toast.makeText(requireContext(), R.string.profile_save_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
