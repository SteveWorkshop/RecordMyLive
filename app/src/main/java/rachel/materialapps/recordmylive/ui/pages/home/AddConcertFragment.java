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
import android.text.InputFilter;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.textfield.TextInputLayout;

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

public class AddConcertFragment extends Fragment {

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

    private Integer selectedDurationMinutes;

    private ConcertType selectedConcertType;
    private ConcertStatus selectedConcertStatus = ConcertStatus.PLANNED;
    private boolean editMode;
    private boolean formPopulated;
    private ConcertEntity originalConcert;
    private Uri selectedCoverUri;
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
            String date = savedInstanceState.getString("selectedDate");
            String time = savedInstanceState.getString("selectedTime");
            String type = savedInstanceState.getString("selectedConcertType");
            selectedDate = date == null ? null : LocalDate.parse(date);
            selectedTime = time == null ? null : LocalTime.parse(time);
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
        if (selectedConcertType != null) {
            binding.layoutBasicInfo.actType.setText(selectedConcertType.getDisplayName(), false);
        }
        binding.layoutBasicInfo.actStatus.setText(selectedConcertStatus.getDisplayName(), false);
        updateDurationText();
        initToolbar();
        editMode = getArguments() != null && getArguments().containsKey("concertId");
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
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("selectedDate", selectedDate == null ? null : selectedDate.toString());
        outState.putString("selectedTime", selectedTime == null ? null : selectedTime.toString());
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
        binding.layoutCover.imgCover.setImageURI(selectedCoverUri);
        binding.layoutCover.coverPlaceholder.setVisibility(View.GONE);
        binding.layoutCover.tvCoverHint.setText(editMode
                ? "点击图片可更换图片" : "更换封面");
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
            EditText input = new EditText(requireContext());
            input.setId(R.id.edtArtistName);
            input.setSingleLine(true);
            input.setFilters(new InputFilter[]{(source, start, end, dest, dstart, dend) -> {
                String filtered = ArtistNameNormalizer.normalize(source.subSequence(start, end).toString());
                return filtered.equals(source.subSequence(start, end).toString())
                        ? null : filtered;
            }});
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
            EditText name = new EditText(requireContext());
            name.setId(R.id.edtSongName);
            name.setSingleLine(true);
            name.setText(song.getName());
            nameLayout.addView(name, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            row.addView(nameLayout, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout controls = new LinearLayout(requireContext());
            controls.setGravity(android.view.Gravity.CENTER_VERTICAL);
            CheckBox encore = new CheckBox(requireContext());
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
        Button button = new Button(requireContext());
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
