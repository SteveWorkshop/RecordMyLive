package rachel.materialapps.recordmylive.ui.pages.home;

import androidx.lifecycle.ViewModelProvider;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textview.MaterialTextView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentAddConcertBinding;
import rachel.materialapps.recordmylive.ui.components.ChinaRegionPicker;
import rachel.materialapps.recordmylive.logic.constants.AppConstants;
import rachel.materialapps.recordmylive.logic.constants.ConcertStatus;
import rachel.materialapps.recordmylive.logic.constants.ConcertType;
import rachel.materialapps.recordmylive.logic.constants.PurchaseChannel;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.SeatInfo;
import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;
import rachel.materialapps.recordmylive.logic.util.ArtistNameNormalizer;
import rachel.materialapps.recordmylive.logic.util.TicketPrice;
import rachel.materialapps.recordmylive.logic.importer.ConcertImportResult;
import com.bumptech.glide.Glide;

public class AddConcertFragment extends Fragment {
    public static final String ARG_SHARED_CONCERT_TEXT = "sharedConcertText";
    private static final String STATE_SHARED_IMPORT_STARTED = "sharedImportStarted";
    private boolean sharedImportStarted;

    @Getter
    @Setter
    private NavHostFragment navHostFragment;

    @Getter
    @Setter
    private NavController navController;


    private FragmentAddConcertBinding binding;

    private AddConcertViewModel mViewModel;


    private LocalDate selectedDate;

    private LocalTime selectedTime;
    private LocalDate selectedTicketSaleDate;
    private LocalTime selectedTicketSaleTime;
    private boolean ticketSaleRestored;

    private Integer selectedDurationMinutes;

    private ConcertType selectedConcertType;
    private ConcertStatus selectedConcertStatus = ConcertStatus.PLANNED;
    private boolean editMode;
    private boolean formPopulated;
    private ConcertEntity originalConcert;
    private Uri selectedCoverUri;
    private androidx.appcompat.app.AlertDialog importDialog;
    private ActivityResultLauncher<String[]> coverPicker;
    private final List<ConcertSongEntity> songs = new ArrayList<>();
    private final List<String> artists = new ArrayList<>();

    public static AddConcertFragment newInstance() {
        return new AddConcertFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding=FragmentAddConcertBinding.inflate(inflater,container,false);

        coverPicker = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null && binding != null) {
                selectedCoverUri = uri;
                try {
                    requireContext().getContentResolver().takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } catch (SecurityException ignored) {
                    // Some document providers do not offer persistable URI permissions.
                }
                binding.layoutCover.imgCover.setImageURI(uri);
                binding.layoutCover.coverPlaceholder.setVisibility(View.GONE);
                binding.layoutCover.tvCoverHint.setText(editMode
                        ? "点击图片可更换图片" : "更换封面");
            }
        });
        binding.layoutCover.cardCover.setOnClickListener(v -> coverPicker.launch(new String[]{"image/*"}));
        binding.layoutSongList.btnAddSong.setOnClickListener(v -> {
            syncSongsFromViews();
            if (songs.size() >= AppConstants.MAX_SONG_COUNT) {
                Snackbar.make(binding.getRoot(), "每场最多记录" + AppConstants.MAX_SONG_COUNT
                        + "首歌曲", Snackbar.LENGTH_LONG).show();
                return;
            }
            ConcertSongEntity song = new ConcertSongEntity("");
            songs.add(song);
            renderSongs();
            if (binding.layoutSongList.songRows.getChildCount() > 0) {
                EditText name = binding.layoutSongList.songRows.getChildAt(
                        binding.layoutSongList.songRows.getChildCount() - 1).findViewById(R.id.edtSongName);
                name.requestFocus();
            }
        });
        binding.layoutArtistList.btnAddArtist.setOnClickListener(v -> {
            syncArtistsFromViews();
            if (artists.size() >= AppConstants.MAX_ARTIST_COUNT) {
                Snackbar.make(binding.getRoot(), "每场最多记录" + AppConstants.MAX_ARTIST_COUNT
                        + "位歌手", Snackbar.LENGTH_LONG).show();
                return;
            }
            artists.add("");
            renderArtists();
            EditText name = binding.layoutArtistList.artistRows.getChildAt(
                    binding.layoutArtistList.artistRows.getChildCount() - 1)
                    .findViewById(R.id.edtArtistName);
            name.requestFocus();
        });
        binding.layoutTicketPrice.checkPricesEqual.setOnCheckedChangeListener((button, checked) -> {
            if (checked) {
                String paid = binding.layoutTicketPrice.edtPaidPrice.getText().toString();
                binding.layoutTicketPrice.edtSinglePrice.setText(paid.isEmpty()
                        ? binding.layoutTicketPrice.edtFaceValuePrice.getText() : paid);
                binding.layoutTicketPrice.layoutSinglePrice.setVisibility(View.VISIBLE);
                binding.layoutTicketPrice.layoutDifferentPrices.setVisibility(View.GONE);
            } else {
                String single = binding.layoutTicketPrice.edtSinglePrice.getText().toString();
                binding.layoutTicketPrice.edtFaceValuePrice.setText(single);
                binding.layoutTicketPrice.edtPaidPrice.setText(single);
                binding.layoutTicketPrice.layoutSinglePrice.setVisibility(View.GONE);
                binding.layoutTicketPrice.layoutDifferentPrices.setVisibility(View.VISIBLE);
            }
        });

        //binding.toolbar.inflateMenu(R.menu.menu_save);

        binding.layoutBasicInfo.edtDate.setOnClickListener(v->{
            showDatePicker();
        });

        binding.layoutBasicInfo.edtTime.setOnClickListener(v -> {

            showTimePicker();

        });

        binding.layoutBasicInfo.edtTicketSaleDate.setOnClickListener(v -> showTicketSaleDatePicker());
        binding.layoutBasicInfo.layoutTicketSaleDate.setEndIconOnClickListener(v -> {
            selectedTicketSaleDate = null;
            selectedTicketSaleTime = null;
            binding.layoutBasicInfo.layoutTicketSaleDate.setError(null);
            showTicketSaleSelection();
        });
        binding.layoutBasicInfo.layoutTicketSaleTime.setEndIconOnClickListener(v -> {
            selectedTicketSaleTime = null;
            showTicketSaleSelection();
        });
        binding.layoutBasicInfo.edtTicketSaleTime.setOnClickListener(v -> {
            if (selectedTicketSaleDate == null) {
                binding.layoutBasicInfo.layoutTicketSaleDate.setError("请先选择开票日期");
            } else {
                showTicketSaleTimePicker();
            }
        });

        binding.layoutBasicInfo.edtDuration.setOnClickListener(v -> showDurationPicker());

        binding.layoutLocation.edtCity.setOnClickListener(v ->
                ChinaRegionPicker.show(requireContext(), selected -> {
                    if (binding != null) {
                        binding.layoutLocation.layoutCity.setError(null);
                        binding.layoutLocation.edtCity.setText(selected);
                    }
                }));

        ConcertType[] values = ConcertType.values();

        String[] names = new String[values.length];

        for (int i = 0; i < values.length; i++) {
            names[i] = values[i].getDisplayName();
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_list_item_1,
                        names
                );

        binding.layoutBasicInfo.actType.setAdapter(adapter);

        binding.layoutBasicInfo.actType.setOnItemClickListener(
                (parent, view, position, id) ->
                        selectedConcertType = values[position]
        );

        ConcertStatus[] statusValues = ConcertStatus.values();
        String[] statusNames = new String[statusValues.length];
        for (int i = 0; i < statusValues.length; i++) {
            statusNames[i] = statusValues[i].getDisplayName();
        }
        binding.layoutBasicInfo.actStatus.setAdapter(new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_list_item_1, statusNames));
        binding.layoutBasicInfo.actStatus.setText(selectedConcertStatus.getDisplayName(), false);
        binding.layoutBasicInfo.actStatus.setOnItemClickListener(
                (parent, statusView, position, id) -> selectedConcertStatus = statusValues[position]);

        View root=binding.getRoot();

        return root;//inflater.inflate(R.layout.fragment_add_concert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(AddConcertViewModel.class);
        if (savedInstanceState != null) {
            sharedImportStarted = savedInstanceState.getBoolean(STATE_SHARED_IMPORT_STARTED, false);
            String date = savedInstanceState.getString("selectedDate");
            String time = savedInstanceState.getString("selectedTime");
            String type = savedInstanceState.getString("selectedConcertType");
            selectedDate = date == null ? null : LocalDate.parse(date);
            selectedTime = time == null ? null : LocalTime.parse(time);
            String ticketDate = savedInstanceState.getString("selectedTicketSaleDate");
            String ticketTime = savedInstanceState.getString("selectedTicketSaleTime");
            selectedTicketSaleDate = ticketDate == null ? null : LocalDate.parse(ticketDate);
            selectedTicketSaleTime = ticketTime == null ? null : LocalTime.parse(ticketTime);
            ticketSaleRestored = true;
            selectedConcertType = type == null ? null : ConcertType.valueOf(type);
            String status = savedInstanceState.getString("selectedConcertStatus");
            selectedConcertStatus = status == null
                    ? ConcertStatus.PLANNED : ConcertStatus.valueOf(status);
            if (savedInstanceState.containsKey("selectedDurationMinutes")) {
                selectedDurationMinutes = savedInstanceState.getInt("selectedDurationMinutes");
            }
            String coverUri = savedInstanceState.getString("selectedCoverUri");
            selectedCoverUri = coverUri == null ? null : Uri.parse(coverUri);
            if (selectedCoverUri != null) {
                showSelectedCover();
            }
            @SuppressWarnings("unchecked")
            ArrayList<ConcertSongEntity> restoredSongs =
                    (ArrayList<ConcertSongEntity>) savedInstanceState.getSerializable("songs");
            if (restoredSongs != null) {
                songs.addAll(restoredSongs);
                renderSongs();
            }
            ArrayList<String> restoredArtists = savedInstanceState.getStringArrayList("artists");
            if (restoredArtists != null) {
                artists.addAll(restoredArtists);
                renderArtists();
            }
        } else if (getArguments() != null && getArguments().containsKey("selectedDate")) {
            selectedDate = LocalDate.parse(requireArguments().getString("selectedDate"));
        }
        if (selectedDate != null) {
            binding.layoutBasicInfo.edtDate.setText(selectedDate.toString());
        }
        if (selectedTime != null) {
            binding.layoutBasicInfo.edtTime.setText(selectedTime.toString());
        }
        showTicketSaleSelection();
        if (selectedConcertType != null) {
            binding.layoutBasicInfo.actType.setText(selectedConcertType.getDisplayName(), false);
        }
        binding.layoutBasicInfo.actStatus.setText(selectedConcertStatus.getDisplayName(), false);
        updateDurationText();
        initToolbar();
        editMode = getArguments() != null && getArguments().containsKey("concertId");
        binding.toolbar.getMenu().findItem(R.id.action_import_link).setVisible(!editMode);
        mViewModel.getLinkImportState().observe(getViewLifecycleOwner(), state -> {
            binding.importProgress.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            binding.toolbar.getMenu().findItem(R.id.action_import_link)
                    .setEnabled(!state.loading && state.result == null);
            binding.toolbar.getMenu().findItem(R.id.action_import_link)
                    .setTitle(state.loading ? "正在解析链接…" : "从链接导入");
            updateSaveAvailability();
            if (state.error != null) {
                Snackbar.make(binding.getRoot(), state.error, Snackbar.LENGTH_LONG).show();
                mViewModel.finishLinkImport(false);
            } else if (state.result != null && !editMode) {
                showImportPreview(state.result);
            }
        });
        if (editMode) {
            binding.toolbar.setTitle("编辑演出");
            binding.layoutCover.tvCoverHint.setText("点击图片可更换图片");
            mViewModel.getConcertDetail().observe(getViewLifecycleOwner(), this::populateForm);
            mViewModel.loadConcert(requireArguments().getLong("concertId"));
        }
        navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager().findFragmentById(R.id.my_nav_host_fragment);
        navController = navHostFragment.getNavController();

        mViewModel.getSaveState().observe(getViewLifecycleOwner(), state -> {
            boolean saving = state == AddConcertViewModel.SaveState.SAVING;
            binding.toolbar.getMenu().findItem(R.id.action_save).setEnabled(!saving);
            updateSaveAvailability();
            binding.toolbar.getMenu().findItem(R.id.action_save)
                    .setTitle(saving ? "保存中…" : "保存");
            switch (state) {
                case SUCCESS:
                    if (!editMode) {
                        NavHostFragment.findNavController(this).getPreviousBackStackEntry()
                                .getSavedStateHandle().set("selectedDate", selectedDate.toString());
                    }
                    mViewModel.resetSaveState();
                    Toast.makeText(requireContext(), "保存成功！", Toast.LENGTH_SHORT).show();
                    navController.navigateUp();
                    break;
                case DAILY_LIMIT_REACHED:
                    mViewModel.resetSaveState();
                    Snackbar.make(binding.getRoot(), "当天最多只能记录"
                            + AppConstants.MAX_CONCERT_PER_DAY + "场演出", Snackbar.LENGTH_LONG).show();
                    break;
                case ERROR:
                    mViewModel.resetSaveState();
                    Snackbar.make(binding.getRoot(), "保存失败，请重试", Snackbar.LENGTH_LONG).show();
                    break;
                default:
                    break;
            }
        });
        if (!editMode && !sharedImportStarted && getArguments() != null) {
            String sharedText = getArguments().getString(ARG_SHARED_CONCERT_TEXT);
            if (sharedText != null && !sharedText.trim().isEmpty()) {
                sharedImportStarted = true;
                mViewModel.importLink(sharedText);
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        AddConcertViewModel.LinkImportState importState = mViewModel.getLinkImportState().getValue();
        // A pending request/result lives in the ViewModel across rotation. If the process
        // dies instead, restart it from the saved share rather than losing the import.
        outState.putBoolean(STATE_SHARED_IMPORT_STARTED, sharedImportStarted
                && (importState == null || (!importState.loading && importState.result == null)));
        outState.putString("selectedDate", selectedDate == null ? null : selectedDate.toString());
        outState.putString("selectedTime", selectedTime == null ? null : selectedTime.toString());
        outState.putString("selectedTicketSaleDate", selectedTicketSaleDate == null
                ? null : selectedTicketSaleDate.toString());
        outState.putString("selectedTicketSaleTime", selectedTicketSaleTime == null
                ? null : selectedTicketSaleTime.toString());
        outState.putString("selectedConcertType", selectedConcertType == null ? null : selectedConcertType.name());
        outState.putString("selectedConcertStatus", selectedConcertStatus.name());
        if (selectedDurationMinutes != null) {
            outState.putInt("selectedDurationMinutes", selectedDurationMinutes);
        }
        outState.putString("selectedCoverUri", selectedCoverUri == null ? null : selectedCoverUri.toString());
        syncSongsFromViews();
        outState.putSerializable("songs", new ArrayList<>(songs));
        syncArtistsFromViews();
        outState.putStringArrayList("artists", new ArrayList<>(artists));
    }

    @Override
    public void onDestroyView() {
        if (importDialog != null) {
            importDialog.setOnCancelListener(null);
            importDialog.dismiss();
            importDialog = null;
        }
        super.onDestroyView();
        binding = null;
    }

    private void showDatePicker(){
        MaterialDatePicker<Long> picker =
                MaterialDatePicker.Builder.datePicker()
                        .setTitleText("选择演出日期")
                        .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (binding == null) {
                return;
            }

            selectedDate = Instant.ofEpochMilli(selection)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate();

            binding.layoutBasicInfo.edtDate.setText(
                    selectedDate.format(DateTimeFormatter.ISO_DATE)
            );

        });

        picker.show(getParentFragmentManager(), "datePicker");
    }

    private void showTimePicker(){
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setHour(19)
                .setMinute(30)
                .build();

        picker.addOnPositiveButtonClickListener(v -> {
            if (binding == null) {
                return;
            }

            selectedTime = LocalTime.of(
                    picker.getHour(),
                    picker.getMinute()
            );

            binding.layoutBasicInfo.edtTime.setText(
                    selectedTime.toString()
            );

        });

        picker.show(getParentFragmentManager(), "timePicker");
    }

    private void showTicketSaleDatePicker() {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择开票日期")
                .setSelection(selectedTicketSaleDate == null ? MaterialDatePicker.todayInUtcMilliseconds()
                        : selectedTicketSaleDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (binding == null) return;
            selectedTicketSaleDate = Instant.ofEpochMilli(selection).atZone(ZoneOffset.UTC).toLocalDate();
            binding.layoutBasicInfo.layoutTicketSaleDate.setError(null);
            showTicketSaleSelection();
        });
        picker.show(getParentFragmentManager(), "ticketSaleDatePicker");
    }

    private void showTicketSaleTimePicker() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setHour(selectedTicketSaleTime == null ? 10 : selectedTicketSaleTime.getHour())
                .setMinute(selectedTicketSaleTime == null ? 0 : selectedTicketSaleTime.getMinute())
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            if (binding == null) return;
            selectedTicketSaleTime = LocalTime.of(picker.getHour(), picker.getMinute());
            showTicketSaleSelection();
        });
        picker.show(getParentFragmentManager(), "ticketSaleTimePicker");
    }

    private void showTicketSaleSelection() {
        binding.layoutBasicInfo.edtTicketSaleDate.setText(selectedTicketSaleDate == null
                ? "" : selectedTicketSaleDate.toString());
        binding.layoutBasicInfo.edtTicketSaleTime.setText(selectedTicketSaleTime == null
                ? "" : selectedTicketSaleTime.toString());
    }


    private boolean validateInput() {

        boolean valid = true;

        if (binding.layoutBasicInfo.edtName.getText().toString().trim().isEmpty()) {

            binding.layoutBasicInfo.layoutName.setError("请输入演出名称");

            valid = false;

        } else {

            binding.layoutBasicInfo.layoutName.setError(null);

        }

        if (selectedDate == null) {

            binding.layoutBasicInfo.layoutDate.setError("请选择日期");

            valid = false;

        } else {

            binding.layoutBasicInfo.layoutDate.setError(null);

        }

        if (selectedConcertType == null) {
            binding.layoutBasicInfo.layoutType.setError("请选择演出类型");
            valid = false;
        } else {
            binding.layoutBasicInfo.layoutType.setError(null);
        }

        if (binding.layoutLocation.edtCity.getText().toString().trim().isEmpty()) {

            binding.layoutLocation.layoutCity.setError("请输入城市");

            valid = false;

        } else {

            binding.layoutLocation.layoutCity.setError(null);

        }

        if (binding.layoutTicketPrice.checkPricesEqual.isChecked()) {
            boolean priceValid = TicketPrice.parseYuanToCents(
                    binding.layoutTicketPrice.edtSinglePrice.getText().toString()) != null;
            binding.layoutTicketPrice.layoutSinglePrice.setError(priceValid
                    ? null : "请输入非负金额，最多两位小数");
            valid &= priceValid;
        } else {
            boolean faceValid = TicketPrice.parseYuanToCents(
                    binding.layoutTicketPrice.edtFaceValuePrice.getText().toString()) != null;
            boolean paidValid = TicketPrice.parseYuanToCents(
                    binding.layoutTicketPrice.edtPaidPrice.getText().toString()) != null;
            binding.layoutTicketPrice.layoutFaceValuePrice.setError(faceValid
                    ? null : "请输入非负金额，最多两位小数");
            binding.layoutTicketPrice.layoutPaidPrice.setError(paidValid
                    ? null : "请输入非负金额，最多两位小数");
            valid &= faceValid && paidValid;
        }

        return valid;
    }

    private void initToolbar() {

        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );

        binding.toolbar.setOnMenuItemClickListener(item -> {

            if (item.getItemId() == R.id.action_import_link) {
                showLinkImportDialog();
                return true;
            }

            if (item.getItemId() == R.id.action_save) {
                saveConcert();
                return true;
            }

            return false;
        });
    }

    private void populateForm(ConcertWithSongs detail) {
        if (binding == null || formPopulated || detail == null || detail.concert == null) {
            return;
        }
        originalConcert = detail.concert;
        binding.layoutBasicInfo.edtName.setText(originalConcert.getName());
        binding.layoutNote.edtNote.setText(originalConcert.getNote());
        selectedDate = originalConcert.getDate();
        selectedTime = originalConcert.getTime();
        if (!ticketSaleRestored) {
            selectedTicketSaleDate = originalConcert.getTicketSaleDate();
            selectedTicketSaleTime = originalConcert.getTicketSaleTime();
        }
        selectedConcertType = originalConcert.getType();
        selectedConcertStatus = originalConcert.getStatus() == null
                ? ConcertStatus.PLANNED : originalConcert.getStatus();
        if (selectedCoverUri == null && originalConcert.getCoverPath() != null
                && !originalConcert.getCoverPath().isEmpty()) {
            selectedCoverUri = Uri.parse(originalConcert.getCoverPath());
            showSelectedCover();
        }
        binding.layoutBasicInfo.edtDate.setText(selectedDate == null ? "" : selectedDate.toString());
        binding.layoutBasicInfo.edtTime.setText(selectedTime == null ? "" : selectedTime.toString());
        showTicketSaleSelection();
        Integer durationMinutes = originalConcert.getDurationMinutes();
        if (durationMinutes != null && durationMinutes > 0) {
            selectedDurationMinutes = durationMinutes;
            updateDurationText();
        }
        binding.layoutBasicInfo.actType.setText(selectedConcertType == null
                ? "" : selectedConcertType.getDisplayName(), false);
        binding.layoutBasicInfo.actStatus.setText(selectedConcertStatus.getDisplayName(), false);
        boolean pricesEqual = originalConcert.getFaceValuePriceCents()
                == originalConcert.getPaidPriceCents();
        binding.layoutTicketPrice.checkPricesEqual.setChecked(pricesEqual);
        if (pricesEqual) {
            binding.layoutTicketPrice.edtSinglePrice.setText(
                    TicketPrice.formatCentsAsYuan(originalConcert.getPaidPriceCents()));
            binding.layoutTicketPrice.layoutSinglePrice.setVisibility(View.VISIBLE);
            binding.layoutTicketPrice.layoutDifferentPrices.setVisibility(View.GONE);
        } else {
            binding.layoutTicketPrice.edtFaceValuePrice.setText(
                    TicketPrice.formatCentsAsYuan(originalConcert.getFaceValuePriceCents()));
            binding.layoutTicketPrice.edtPaidPrice.setText(
                    TicketPrice.formatCentsAsYuan(originalConcert.getPaidPriceCents()));
            binding.layoutTicketPrice.layoutSinglePrice.setVisibility(View.GONE);
            binding.layoutTicketPrice.layoutDifferentPrices.setVisibility(View.VISIBLE);
        }
        binding.layoutLocation.edtCity.setText(originalConcert.getCity());
        binding.layoutLocation.edtVenue.setText(originalConcert.getVenue());
        SeatInfo seat = originalConcert.getSeat();
        binding.layoutSeatInfo.edtSeatSection.setText(seat == null ? "" : seat.getSection());
        binding.layoutSeatInfo.edtSeatRow.setText(seat == null ? "" : seat.getRow());
        binding.layoutSeatInfo.edtSeatNumber.setText(seat == null ? "" : seat.getNumber());
        songs.clear();
        if (detail.songs != null) {
            songs.addAll(detail.songs);
        }
        renderSongs();
        artists.clear();
        if (originalConcert.getArtists() != null) {
            for (String artist : originalConcert.getArtists()) {
                String normalized = ArtistNameNormalizer.normalize(artist);
                if (!normalized.isEmpty()) artists.add(normalized);
            }
        }
        renderArtists();
        formPopulated = true;
    }

    private void showSelectedCover() {
        if (binding == null || selectedCoverUri == null) {
            return;
        }
        Glide.with(this).load(selectedCoverUri).into(binding.layoutCover.imgCover);
        binding.layoutCover.coverPlaceholder.setVisibility(View.GONE);
        binding.layoutCover.tvCoverHint.setText(editMode
                ? "点击图片可更换图片" : "更换封面");
    }

    private void updateSaveAvailability() {
        if (binding == null || mViewModel == null) return;
        AddConcertViewModel.LinkImportState state = mViewModel.getLinkImportState().getValue();
        boolean importing = state != null && (state.loading || state.result != null);
        boolean saving = mViewModel.getSaveState().getValue() == AddConcertViewModel.SaveState.SAVING;
        binding.toolbar.getMenu().findItem(R.id.action_save).setEnabled(!saving && !importing);
        binding.toolbar.getMenu().findItem(R.id.action_import_link).setEnabled(!saving && !importing);
    }

    private void showLinkImportDialog() {
        if (editMode || binding == null) return;
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setHint("链接或分享文案");
        layout.setPadding(dp(24), dp(8), dp(24), 0);
        EditText input = new TextInputEditText(requireContext());
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMinLines(2);
        layout.addView(input);
        importDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("从链接导入")
                .setMessage("粘贴演出链接或分享文案，自动识别平台。")
                .setView(layout)
                .setNegativeButton("取消", null)
                .setPositiveButton("解析", null).create();
        importDialog.setOnShowListener(dialog -> importDialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String text = input.getText().toString().trim();
            if (text.isEmpty()) { layout.setError("请粘贴演出链接"); return; }
            importDialog.dismiss();
            importDialog = null;
            mViewModel.importLink(text);
        }));
        importDialog.show();
    }

    private void showImportPreview(ConcertImportResult result) {
        if (importDialog != null && importDialog.isShowing()) return;
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(8), dp(24), dp(8));
        TextView summary = new MaterialTextView(requireContext());
        String text = "来源：" + result.platform + "\n" + result.name + "\n" + result.city + " · " + result.venue
                + (result.artists.isEmpty() ? "" : "\n歌手：" + android.text.TextUtils.join("、", result.artists))
                + (result.durationMinutes == null ? "" : "\n时长：" + result.durationMinutes + "分钟")
               // + (result.priceRange == null || result.priceRange.isEmpty() ? "" : "\n参考票价：" + result.priceRange + "元（不填入个人票价）")
                //+ "\n\n所选场次将更新演出日期和时间；切换日期会清空原开票时间。其他信息只补充空白项。"
                ;
        if (!result.warnings.isEmpty()) text += "\n\n" + android.text.TextUtils.join("\n", result.warnings);
        summary.setText(text);
        content.addView(summary);
        final int[] selectedPerformanceIndex = {0};
        int defaultIndex = 0;
        if (!result.performances.isEmpty()) {
            List<String> labels = new ArrayList<>();
            for (int i = 0; i < result.performances.size(); i++) {
                ConcertImportResult.Performance p = result.performances.get(i);
                labels.add(p.label());
                if (p.date.equals(selectedDate)) defaultIndex = i;
            }
            selectedPerformanceIndex[0] = defaultIndex;
            TextInputLayout selectionLayout = new TextInputLayout(requireContext());
            selectionLayout.setHint("演出场次");
            selectionLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
            MaterialAutoCompleteTextView dropdown = new MaterialAutoCompleteTextView(requireContext());
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_list_item_1, labels);
            dropdown.setAdapter(adapter);
            dropdown.setText(labels.get(defaultIndex), false);
            dropdown.setOnItemClickListener((parent, view, position, id) ->
                    selectedPerformanceIndex[0] = position);
            selectionLayout.addView(dropdown, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            content.addView(selectionLayout, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        }
        android.widget.ScrollView scroll = new android.widget.ScrollView(requireContext());
        scroll.addView(content);
        importDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(result.performances.isEmpty() ? "确认导入信息" : "选择演出场次")
                .setView(scroll)
                .setNegativeButton("取消", (dialog, which) -> {
                    importDialog = null;
                    mViewModel.finishLinkImport(false);
                })
                .setPositiveButton("填入表单", (dialog, which) -> {
                    ConcertImportResult.Performance performance = result.performances.isEmpty()
                            ? null : result.performances.get(selectedPerformanceIndex[0]);
                    applyImportedConcert(result, performance);
                    importDialog = null;
                })
                .setOnCancelListener(dialog -> {
                    importDialog = null;
                    mViewModel.finishLinkImport(false);
                }).create();
        importDialog.show();
    }

    private void applyImportedConcert(ConcertImportResult result, ConcertImportResult.Performance performance) {
        if (binding == null) return;
        fillEmpty(binding.layoutBasicInfo.edtName, result.name);
        fillEmpty(binding.layoutLocation.edtCity, result.city);
        fillEmpty(binding.layoutLocation.edtVenue, result.venue);
        if (selectedConcertType == null && result.type != null) {
            selectedConcertType = result.type;
            binding.layoutBasicInfo.actType.setText(result.type.getDisplayName(), false);
        }
        if (selectedDurationMinutes == null && result.durationMinutes != null) {
            selectedDurationMinutes = result.durationMinutes;
            updateDurationText();
        }
        if (performance != null) {
            if (selectedDate != null && !selectedDate.equals(performance.date)) {
                selectedTicketSaleDate = null;
                selectedTicketSaleTime = null;
            }
            selectedDate = performance.date;
            selectedTime = performance.time;
            binding.layoutBasicInfo.edtDate.setText(selectedDate.toString());
            binding.layoutBasicInfo.edtTime.setText(selectedTime == null ? "" : selectedTime.toString());
            binding.layoutBasicInfo.layoutDate.setError(null);
            if (selectedTicketSaleDate == null) {
                selectedTicketSaleDate = performance.saleDate;
                selectedTicketSaleTime = performance.saleTime;
            }
            showTicketSaleSelection();
        }
        syncArtistsFromViews();
        if (artists.stream().allMatch(name -> name == null || name.trim().isEmpty())) {
            artists.clear();
            for (String name : result.artists) {
                if (artists.size() >= AppConstants.MAX_ARTIST_COUNT) break;
                String normalized = ArtistNameNormalizer.normalize(name);
                if (!normalized.isEmpty() && !artists.contains(normalized)) artists.add(normalized);
            }
            renderArtists();
        }
        boolean keepCover = selectedCoverUri == null && result.localCoverPath != null;
        if (keepCover) {
            selectedCoverUri = Uri.fromFile(new java.io.File(result.localCoverPath));
            showSelectedCover();
        }
        fillEmpty(binding.layoutNote.edtNote, "来源：" + result.platform + "\n" + result.sourceUrl);
        mViewModel.finishLinkImport(keepCover);
        Snackbar.make(binding.getRoot(), "已填入解析信息",
                Snackbar.LENGTH_LONG).show();
    }

    private void fillEmpty(EditText input, String value) {
        if (input.getText().toString().trim().isEmpty() && value != null && !value.isEmpty()) input.setText(value);
    }

    private ConcertEntity buildConcert(){
        ConcertEntity concert = editMode ? originalConcert : ConcertEntity.createEmpty();
        if (concert == null) {
            concert = ConcertEntity.createEmpty();
        }
        concert.setName(binding.layoutBasicInfo.edtName.getText().toString().trim());
        concert.setType(selectedConcertType);
        concert.setDate(selectedDate);
        concert.setTime(selectedTime);
        concert.setTicketSaleDate(selectedTicketSaleDate);
        concert.setTicketSaleTime(selectedTicketSaleTime);
        concert.setDurationMinutes(selectedDurationMinutes);
        concert.setStatus(selectedConcertStatus);
        concert.setCity(binding.layoutLocation.edtCity.getText().toString().trim());
        concert.setVenue(binding.layoutLocation.edtVenue.getText().toString().trim());
        long faceValuePriceCents;
        long paidPriceCents;
        if (binding.layoutTicketPrice.checkPricesEqual.isChecked()) {
            faceValuePriceCents = TicketPrice.parseYuanToCents(
                    binding.layoutTicketPrice.edtSinglePrice.getText().toString());
            paidPriceCents = faceValuePriceCents;
        } else {
            faceValuePriceCents = TicketPrice.parseYuanToCents(
                    binding.layoutTicketPrice.edtFaceValuePrice.getText().toString());
            paidPriceCents = TicketPrice.parseYuanToCents(
                    binding.layoutTicketPrice.edtPaidPrice.getText().toString());
        }
        concert.setFaceValuePriceCents(faceValuePriceCents);
        concert.setPaidPriceCents(paidPriceCents);
        SeatInfo seat = new SeatInfo();
        seat.setSection(emptyToNull(binding.layoutSeatInfo.edtSeatSection.getText().toString()));
        seat.setRow(emptyToNull(binding.layoutSeatInfo.edtSeatRow.getText().toString()));
        seat.setNumber(emptyToNull(binding.layoutSeatInfo.edtSeatNumber.getText().toString()));
        concert.setSeat(seat);

        if (!editMode) {
            concert.setPurchaseChannel(PurchaseChannel.OTHER);
            concert.setFavorite(false);
            concert.setTicketCollected(false);
            concert.setNote(null);
        }
        syncArtistsFromViews();
        List<String> savedArtists = new ArrayList<>();
        for (String artist : artists) {
            if (artist != null && !artist.trim().isEmpty()) {
            savedArtists.add(ArtistNameNormalizer.normalize(artist));
            }
        }
        concert.setArtists(savedArtists);
        concert.setCoverPath(selectedCoverUri == null ? concert.getCoverPath() : selectedCoverUri.toString());
        concert.setNote(emptyToNull(binding.layoutNote.edtNote.getText().toString()));
        return concert;
    }

    private void saveConcert(){
        if(validateInput() && (!editMode || originalConcert != null)){
            syncSongsFromViews();
            List<ConcertSongEntity> songsToSave = new ArrayList<>();
            for (int i = 0; i < songs.size(); i++) {
                ConcertSongEntity song = songs.get(i);
                if (song.getName() != null && !song.getName().trim().isEmpty()) {
                    song.setSortOrder(songsToSave.size() + 1);
                    songsToSave.add(song);
                }
            }
            ConcertEntity concert = buildConcert();
            if (editMode) {
                mViewModel.updateConcert(concert, songsToSave);
            } else {
                mViewModel.saveConcert(concert, songsToSave);
            }
        } else if (editMode && originalConcert == null) {
            Snackbar.make(binding.getRoot(), "演出信息尚未加载，请稍后重试", Snackbar.LENGTH_LONG).show();
        }

    }

    private void syncSongsFromViews() {
        if (binding == null) {
            return;
        }
        LinearLayout rows = binding.layoutSongList.songRows;
        for (int i = 0; i < rows.getChildCount() && i < songs.size(); i++) {
            View row = rows.getChildAt(i);
            songs.get(i).setName(((EditText) row.findViewById(R.id.edtSongName)).getText().toString().trim());
            songs.get(i).setEncore(((CheckBox) row.findViewById(R.id.checkEncore)).isChecked());
        }
    }

    private void syncArtistsFromViews() {
        if (binding == null) return;
        LinearLayout rows = binding.layoutArtistList.artistRows;
        for (int i = 0; i < rows.getChildCount() && i < artists.size(); i++) {
            EditText name = rows.getChildAt(i).findViewById(R.id.edtArtistName);
            artists.set(i, ArtistNameNormalizer.normalize(name.getText().toString()));
        }
    }

    private void renderArtists() {
        if (binding == null) return;
        LinearLayout rows = binding.layoutArtistList.artistRows;
        rows.removeAllViews();
        binding.layoutArtistList.tvEmptyArtists.setVisibility(artists.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < artists.size(); i++) {
            final int position = i;
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            TextInputLayout inputLayout = new TextInputLayout(requireContext());
            inputLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            inputLayout.setHint("歌手名称");
            EditText input = new TextInputEditText(requireContext());
            input.setId(R.id.edtArtistName);
            input.setSingleLine(true);
            input.setText(artists.get(i));
            inputLayout.addView(input, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(inputLayout, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(rowButton("删除", () -> {
                syncArtistsFromViews();
                artists.remove(position);
                renderArtists();
            }));
            rows.addView(row);
        }
    }

    private void renderSongs() {
        if (binding == null) {
            return;
        }
        LinearLayout rows = binding.layoutSongList.songRows;
        rows.removeAllViews();
        binding.layoutSongList.tvEmptySongs.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
        for (int i = 0; i < songs.size(); i++) {
            final int position = i;
            ConcertSongEntity song = songs.get(i);
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            TextInputLayout nameLayout = new TextInputLayout(requireContext());
            nameLayout.setBoxBackgroundMode(TextInputLayout.BOX_BACKGROUND_OUTLINE);
            nameLayout.setHint("歌曲名称");
            EditText name = new TextInputEditText(requireContext());
            name.setId(R.id.edtSongName);
            name.setSingleLine(true);
            name.setText(song.getName());
            nameLayout.addView(name, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(nameLayout, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout controls = new LinearLayout(requireContext());
            controls.setGravity(android.view.Gravity.CENTER_VERTICAL);
            CheckBox encore = new MaterialCheckBox(requireContext());
            encore.setId(R.id.checkEncore);
            encore.setText("返场");
            encore.setChecked(song.isEncore());
            controls.addView(encore, new LinearLayout.LayoutParams(
                    0, dp(48), 1));
            controls.addView(rowButton("↑", () -> moveSong(position, -1)));
            controls.addView(rowButton("↓", () -> moveSong(position, 1)));
            controls.addView(rowButton("删除", () -> {
                syncSongsFromViews();
                songs.remove(position);
                renderSongs();
            }));
            row.addView(controls);
            rows.addView(row);
        }
    }

    private Button rowButton(String label, Runnable action) {
        Button button = new MaterialButton(requireContext());
        button.setText(label);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(40));
        button.setPadding(dp(6), 0, dp(6), 0);
        button.setTextSize(14);
        button.setSingleLine(true);
        button.setLayoutParams(new LinearLayout.LayoutParams(
                label.equals("删除") ? dp(64) : dp(44), dp(44)));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private void moveSong(int position, int offset) {
        syncSongsFromViews();
        int target = position + offset;
        if (target >= 0 && target < songs.size()) {
            Collections.swap(songs, position, target);
            renderSongs();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private String emptyToNull(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void showDurationPicker() {
        View pickerView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_duration_picker, null);
        NumberPicker hoursPicker = pickerView.findViewById(R.id.pickerHours);
        NumberPicker minutesPicker = pickerView.findViewById(R.id.pickerMinutes);
        hoursPicker.setMinValue(0);
        hoursPicker.setMaxValue(99);
        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(59);
        hoursPicker.setValue(selectedDurationMinutes == null ? 2 : selectedDurationMinutes / 60);
        minutesPicker.setValue(selectedDurationMinutes == null ? 0 : selectedDurationMinutes % 60);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("选择演出时长")
                .setView(pickerView)
                .setNeutralButton("清除", (dialog, which) -> {
                    selectedDurationMinutes = null;
                    updateDurationText();
                })
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    int totalMinutes = hoursPicker.getValue() * 60 + minutesPicker.getValue();
                    if (totalMinutes == 0) {
                        Snackbar.make(binding.getRoot(), "演出时长需大于 0 分钟",
                                Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    selectedDurationMinutes = totalMinutes;
                    updateDurationText();
                })
                .show();
    }

    private void updateDurationText() {
        if (binding == null) {
            return;
        }
        if (selectedDurationMinutes == null) {
            binding.layoutBasicInfo.edtDuration.setText("");
            return;
        }
        int hours = selectedDurationMinutes / 60;
        int minutes = selectedDurationMinutes % 60;
        String text = hours == 0 ? minutes + "分钟"
                : minutes == 0 ? hours + "小时"
                : hours + "小时" + minutes + "分钟";
        binding.layoutBasicInfo.edtDuration.setText(text);
    }

}
