package rachel.materialapps.recordmylive.ui.pages.my;

import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentMyBinding;
import rachel.materialapps.recordmylive.databinding.ItemThemeColorBinding;
import rachel.materialapps.recordmylive.ui.ThemeSettings;

public class MyFragment extends Fragment {
    private FragmentMyBinding binding;
    private static final int[] MODE_IDS = {R.id.mode_system, R.id.mode_light, R.id.mode_dark};
    private static final int[] MODES = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};
    private static final int[] NAMES = {R.string.palette_dynamic, R.string.palette_celadon,
            R.string.palette_indigo, R.string.palette_rouge, R.string.palette_sandalwood};
    private static final int[] DESCRIPTIONS = {R.string.palette_dynamic_hint, R.string.palette_celadon_hint,
            R.string.palette_indigo_hint, R.string.palette_rouge_hint, R.string.palette_sandalwood_hint};
    private static final int[] SWATCHES = {0, 0xFF47664F, 0xFF4E607B, 0xFF8C4A5C, 0xFF79583A};

    public static MyFragment newInstance() {
        return new MyFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMyBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        try {
            String versionName = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
            binding.appVersion.setText(versionName == null ? getString(R.string.app_version_unknown)
                    : getString(R.string.app_version, versionName));
        } catch (PackageManager.NameNotFoundException e) {
            binding.appVersion.setText(R.string.app_version_unknown);
        }
        MaterialButtonToggleGroup modes = binding.displayModeGroup;
        int currentMode = ThemeSettings.getNightMode(requireContext());
        modes.check(R.id.mode_system);
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i] == currentMode) modes.check(MODE_IDS[i]);
        }
        modes.addOnButtonCheckedListener((group, checkedId, checked) -> {
            if (!checked) return;
            for (int i = 0; i < MODE_IDS.length; i++) {
                if (MODE_IDS[i] == checkedId) ThemeSettings.setNightMode(requireContext(), MODES[i]);
            }
        });

        GridLayout colors = binding.colorOptions;
        // Keep the choices readable on narrow windows and with large fonts.
        boolean stacked = getResources().getConfiguration().screenWidthDp < 360
                || getResources().getConfiguration().fontScale > 1.3f;
        colors.setColumnCount(stacked ? 1 : 2);
        int selected = ThemeSettings.getColorIndex(requireContext());
        int primary = MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary);
        int outline = MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutlineVariant);
        int surface = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerLow);
        int selectedSurface = MaterialColors.getColor(view, com.google.android.material.R.attr.colorSecondaryContainer);
        for (int i = 0; i < NAMES.length; i++) {
            final int index = i;
            ItemThemeColorBinding colorBinding = ItemThemeColorBinding.inflate(
                    getLayoutInflater(), colors, false);
            MaterialCardView card = colorBinding.getRoot();
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            int row = stacked ? i : (i == 0 ? 0 : (i + 1) / 2);
            int column = stacked || i == 0 ? 0 : (i - 1) % 2;
            int span = !stacked && i == 0 ? 2 : 1;
            params.rowSpec = GridLayout.spec(row);
            params.columnSpec = GridLayout.spec(column, span, 1f);
            params.width = 0;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.setMargins(0, 0, !stacked && i > 0 && column == 0 ? dp(12) : 0, dp(12));
            card.setLayoutParams(params);
            card.setChecked(i == selected);
            card.setStrokeColor(i == selected ? primary : outline);
            card.setStrokeWidth(dp(i == selected ? 2 : 1));
            card.setCardBackgroundColor(i == selected ? selectedSurface : surface);
            colorBinding.colorName.setText(NAMES[i]);
            colorBinding.colorDescription.setText(DESCRIPTIONS[i]);
            card.setContentDescription(getString(ThemeSettings.COLOR_LABELS[i]));
            GradientDrawable swatch = new GradientDrawable();
            swatch.setShape(GradientDrawable.OVAL);
            if (i == 0) {
                swatch.setColors(new int[]{SWATCHES[1], SWATCHES[2], SWATCHES[3], SWATCHES[4]});
                swatch.setOrientation(GradientDrawable.Orientation.TL_BR);
            } else {
                swatch.setColor(SWATCHES[i]);
            }
            colorBinding.colorSwatch.setBackground(swatch);
            card.setOnClickListener(v -> {
                if (ThemeSettings.getColorIndex(requireContext()) == index) return;
                ThemeSettings.setColorIndex(requireContext(), index);
                requireActivity().recreate();
            });
            colors.addView(card);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
