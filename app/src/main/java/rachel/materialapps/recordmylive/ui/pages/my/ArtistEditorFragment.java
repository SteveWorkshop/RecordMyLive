package rachel.materialapps.recordmylive.ui.pages.my;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentArtistEditorBinding;
import rachel.materialapps.recordmylive.logic.util.ArtistImages;

public class ArtistEditorFragment extends Fragment {
    private FragmentArtistEditorBinding binding;
    private ArtistEditorViewModel model;
    // Copy the selected document immediately; no persistent grant or storage permission needed.
    private final ActivityResultLauncher<String[]> picker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) model.chooseImage(uri);
            });

    @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup parent,
                                       @Nullable Bundle savedInstanceState) {
        binding = FragmentArtistEditorBinding.inflate(inflater, parent, false);
        return binding.getRoot();
    }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        model = new ViewModelProvider(this).get(ArtistEditorViewModel.class);
        binding.toolbar.setTitle(model.isEditing() ? R.string.favorite_artist_edit : R.string.favorite_artist_add);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() != R.id.action_save) return false;
            model.save();
            return true;
        });
        binding.artistInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.favorite_artist_suggestions)));
        binding.artistInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.state.set("name", s.toString());
                binding.nameLayout.setError(null);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        binding.artistInput.setOnEditorActionListener((v, action, event) -> {
            if (action != EditorInfo.IME_ACTION_DONE) return false;
            model.save();
            return true;
        });
        binding.chooseImage.setOnClickListener(v -> picker.launch(new String[]{"image/*"}));
        binding.removeImage.setOnClickListener(v -> model.removeImage());
        model.ready.observe(getViewLifecycleOwner(), ready -> {
            if (Boolean.TRUE.equals(ready)) {
                String name = model.state.get("name");
                binding.artistInput.setText(name, false);
            }
            updateEnabled();
        });
        model.busy.observe(getViewLifecycleOwner(), busy -> updateEnabled());
        model.imageVersion.observe(getViewLifecycleOwner(), version -> {
            ArtistImages.showFile(binding.artistImage, model.image());
            binding.removeImage.setVisibility(model.image() == null ? View.GONE : View.VISIBLE);
            binding.chooseImage.setText(model.image() == null ? R.string.favorite_artist_choose_image
                    : R.string.favorite_artist_change_image);
        });
        model.imageError.observe(getViewLifecycleOwner(), error -> {
            if (Boolean.TRUE.equals(error)) {
                model.imageError.setValue(false);
                Toast.makeText(requireContext(), R.string.favorite_artist_image_error, Toast.LENGTH_SHORT).show();
            }
        });
        model.result.observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;
            model.result.setValue(null);
            switch (result) {
                case ADDED: case UPDATED:
                    NavHostFragment.findNavController(this).navigateUp();
                    break;
                case DUPLICATE:
                    binding.nameLayout.setError(getString(R.string.favorite_artist_duplicate));
                    break;
                case INVALID:
                    binding.nameLayout.setError(getString(R.string.favorite_artist_invalid));
                    break;
                default:
                    Toast.makeText(requireContext(), R.string.favorite_artist_error, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateEnabled() {
        boolean enabled = Boolean.TRUE.equals(model.ready.getValue()) && !model.isBusy();
        binding.artistInput.setEnabled(enabled);
        binding.chooseImage.setEnabled(enabled);
        binding.removeImage.setEnabled(enabled);
        binding.toolbar.getMenu().findItem(R.id.action_save).setEnabled(enabled);
        binding.progress.setVisibility(model.isBusy() ? View.VISIBLE : View.GONE);
    }
    @Override public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
