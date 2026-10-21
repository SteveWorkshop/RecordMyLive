package rachel.materialapps.recordmylive.ui.pages.home;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.CalendarContract;
import android.provider.CalendarContract.Events;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.chip.Chip;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentDetailBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ConcertSongEntity;
import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;
import rachel.materialapps.recordmylive.logic.util.TicketPrice;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.util.ExpenseTotals;
import java.util.Collections;

public class DetailFragment extends Fragment {

    private static final String TAG = "DetailFragment";
    private static final String STATE_PENDING_REMINDER_TYPE = "pendingReminderType";

    private FragmentDetailBinding binding;

    private DetailViewModel mViewModel;

    private long concertId;
    private ConcertEntity currentConcert;
    private List<ExpenseEntity> currentExpenses = Collections.emptyList();
    private CalendarReminderWriter.ReminderType pendingReminderType = CalendarReminderWriter.ReminderType.CONCERT;
    private final ExecutorService calendarExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ActivityResultLauncher<String[]> calendarPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (binding == null) return;
                if (Boolean.TRUE.equals(result.get(Manifest.permission.READ_CALENDAR))
                        && Boolean.TRUE.equals(result.get(Manifest.permission.WRITE_CALENDAR))) {
                    chooseCalendar(pendingReminderType);
                } else if (isAdded()) {
                    offerManualCalendar(R.string.calendar_permission_denied, pendingReminderType);
                }
            });

    public static DetailFragment newInstance() {
        return new DetailFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            String type = savedInstanceState.getString(STATE_PENDING_REMINDER_TYPE);
            if (type != null) {
                pendingReminderType = CalendarReminderWriter.ReminderType.valueOf(type);
            }
        }
        binding = FragmentDetailBinding.inflate(inflater, container, false);
        View root=binding.getRoot();
        return root;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString(STATE_PENDING_REMINDER_TYPE, pendingReminderType.name());
        super.onSaveInstanceState(outState);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.toolbar.setNavigationOnClickListener(v ->
                NavHostFragment.findNavController(this).navigateUp()
        );
        binding.toolbar.inflateMenu(R.menu.menu_detail);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_edit) {
                Bundle args = new Bundle();
                args.putLong("concertId", concertId);
                NavHostFragment.findNavController(this).navigate(R.id.action_detail_to_edit, args);
                return true;
            }
            return false;
        });
        concertId = requireArguments().getLong("concertId");
        mViewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        binding.recyclerSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSongs.setNestedScrollingEnabled(false);
        binding.recyclerSongs.setAdapter(new SongAdapter());
        binding.btnCalendarReminder.setOnClickListener(v -> addCalendarReminder(
                CalendarReminderWriter.ReminderType.CONCERT));
        binding.btnTicketSaleReminder.setOnClickListener(v -> addCalendarReminder(
                CalendarReminderWriter.ReminderType.TICKET_SALE));
        mViewModel.getDetail().observe(getViewLifecycleOwner(), this::bindDetail);
        View.OnClickListener openExpenses = v -> {
            if (NavHostFragment.findNavController(this).getCurrentDestination() == null
                    || NavHostFragment.findNavController(this).getCurrentDestination().getId()
                    != R.id.detailFragment) return;
            Bundle args = new Bundle();
            args.putLong("concertId", concertId);
            NavHostFragment.findNavController(this).navigate(R.id.action_detail_to_expenses, args);
        };
        binding.btnViewExpenses.setOnClickListener(openExpenses);
        binding.cardExpenses.setOnClickListener(openExpenses);
        mViewModel.getExpenses().observe(getViewLifecycleOwner(), expenses -> {
            currentExpenses = expenses == null ? Collections.emptyList() : expenses;
            renderExpenses();
        });
        mViewModel.loadConcert(concertId);
    }

    private void bindDetail(ConcertWithSongs detail) {
        if (binding == null || detail == null || detail.concert == null) {
            return;
        }
        ConcertEntity concert = detail.concert;
        currentConcert = concert;
        renderExpenses();
        binding.btnCalendarReminder.setText(CalendarReminderWriter.hasSavedReminder(requireContext(), concert.getId())
                ? R.string.calendar_reminder_update : R.string.calendar_reminder_add);
        binding.btnTicketSaleReminder.setText(CalendarReminderWriter.hasSavedReminder(requireContext(),
                concert.getId(), CalendarReminderWriter.ReminderType.TICKET_SALE)
                ? R.string.ticket_sale_reminder_update : R.string.ticket_sale_reminder_add);
        binding.tvName.setText(concert.getName());
        binding.chipType.setText(concert.getType() == null
                ? "未设置类型" : concert.getType().getDisplayName());

        String time = concert.getDate() == null ? "日期待定" : concert.getDate().toString();
        time += concert.getTime() == null ? " 时间待定" : " " + concert.getTime();
        binding.tvTime.setText(time);

        if (concert.getTicketSaleDate() != null) {
            String ticketSale = "开票时间：" + concert.getTicketSaleDate()
                    + (concert.getTicketSaleTime() == null ? " 时间待定" : " " + concert.getTicketSaleTime());
            binding.tvTicketSale.setText(ticketSale);
            binding.tvTicketSale.setVisibility(View.VISIBLE);
        } else {
            binding.tvTicketSale.setVisibility(View.GONE);
        }

        Integer durationMinutes = concert.getDurationMinutes();
        if (durationMinutes != null && durationMinutes > 0) {
            binding.tvDuration.setText("演出时长：" + formatDuration(durationMinutes));
            binding.tvDuration.setVisibility(View.VISIBLE);
        } else {
            binding.tvDuration.setVisibility(View.GONE);
        }

        String location = concert.getCity() == null ? "" : concert.getCity();
        if (concert.getVenue() != null && !concert.getVenue().isEmpty()) {
            location += (location.isEmpty() ? "" : " · ") + concert.getVenue();
        }
        binding.tvCityVenue.setText(location.isEmpty() ? "未设置地点" : location);

        List<String> seatParts = new ArrayList<>();
        if (concert.getSeat() != null) {
            addSeatPart(seatParts, concert.getSeat().getSection(), "区");
            addSeatPart(seatParts, concert.getSeat().getRow(), "排");
            addSeatPart(seatParts, concert.getSeat().getNumber(), "座");
        }
        binding.tvSeat.setText(seatParts.isEmpty()
                ? "未设置座位" : android.text.TextUtils.join(" ", seatParts));
        if (concert.getFaceValuePriceCents() == concert.getPaidPriceCents()) {
            binding.tvTicketPrice.setText("票面价与实付价一致 · ¥"
                    + TicketPrice.formatCentsAsYuan(concert.getPaidPriceCents()));
        } else {
            binding.tvTicketPrice.setText("票面价 ¥"
                    + TicketPrice.formatCentsAsYuan(concert.getFaceValuePriceCents())
                    + "    实付价 ¥"
                    + TicketPrice.formatCentsAsYuan(concert.getPaidPriceCents()));
        }

        binding.chipArtists.removeAllViews();
        if (concert.getArtists() != null) {
            for (String artist : concert.getArtists()) {
                Chip chip = new Chip(requireContext());
                chip.setText(artist);
                chip.setCheckable(false);
                chip.setClickable(false);
                binding.chipArtists.addView(chip);
            }
        }

        String cover = concert.getCoverPath();
        // 按封面控件尺寸异步解码，避免进入详情时在主线程读取大图。
        Glide.with(this)
                .load(cover == null || cover.trim().isEmpty() ? null : Uri.parse(cover))
                .placeholder(R.drawable.baseline_imagesmode_24px)
                .fallback(R.drawable.baseline_imagesmode_24px)
                .error(R.drawable.baseline_imagesmode_24px)
                .centerCrop()
                .dontAnimate()
                .into(binding.imgCover);
        binding.tvNote.setText(concert.getNote() == null || concert.getNote().isEmpty()
                ? "无备注" : concert.getNote());
        List<ConcertSongEntity> songs = detail.songs == null ? new ArrayList<>() : detail.songs;
        ((SongAdapter) binding.recyclerSongs.getAdapter()).submitList(songs);
        binding.tvSongCount.setText(songs.size() + " 首");
        binding.tvSongsEmpty.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void renderExpenses() {
        if (binding == null || currentConcert == null) return;
        ExpenseTotals totals = ExpenseTotals.calculate(Collections.singletonList(currentConcert), currentExpenses);
        binding.tvExpenseSummary.setText("门票 ¥" + TicketPrice.formatCentsAsYuan(totals.ticketCents)
                + "   附加费用 ¥" + TicketPrice.formatCentsAsYuan(totals.extraCents)
                + "\n总计 ¥" + TicketPrice.formatCentsAsYuan(totals.totalCents));
    }

    private void addCalendarReminder(CalendarReminderWriter.ReminderType type) {
        ConcertEntity concert = currentConcert;
        boolean ticketSale = type == CalendarReminderWriter.ReminderType.TICKET_SALE;
        if (concert == null || (ticketSale
                ? concert.getTicketSaleDate() == null || concert.getTicketSaleTime() == null
                : concert.getDate() == null || concert.getTime() == null)) {
            Toast.makeText(requireContext(), ticketSale ? R.string.ticket_sale_time_required
                    : R.string.calendar_time_required, Toast.LENGTH_SHORT).show();
            return;
        }
        pendingReminderType = type;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED) {
            calendarPermissions.launch(new String[]{Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR});
            return;
        }
        chooseCalendar(type);
    }

    private void chooseCalendar(CalendarReminderWriter.ReminderType type) {
        android.content.Context appContext = requireContext().getApplicationContext();
        calendarExecutor.execute(() -> {
            try {
                List<CalendarReminderWriter.CalendarOption> calendars =
                        CalendarReminderWriter.writableCalendars(appContext);
                mainHandler.post(() -> {
                    if (binding == null) return;
                    if (calendars.isEmpty()) {
                        offerManualCalendar(R.string.calendar_no_writable, type);
                        return;
                    }
                    String[] names = calendars.stream().map(option -> option.label).toArray(String[]::new);
                    long preferredId = CalendarReminderWriter.preferredCalendarId(appContext);
                    int preferredIndex = 0;
                    for (int i = 0; i < calendars.size(); i++) {
                        if (calendars.get(i).id == preferredId) {
                            preferredIndex = i;
                            break;
                        }
                    }
                    final int[] selectedIndex = {preferredIndex};
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.calendar_select)
                            .setSingleChoiceItems(names, preferredIndex,
                                    (dialog, which) -> selectedIndex[0] = which)
                            .setNegativeButton(android.R.string.cancel, null)
                            .setPositiveButton(android.R.string.ok, (dialog, which) ->
                                    chooseReminderTime(calendars.get(selectedIndex[0]).id, type))
                            .show();
                });
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to load writable calendars", e);
                mainHandler.post(() -> {
                    if (binding != null) offerManualCalendar(R.string.calendar_save_failed, type);
                });
            }
        });
    }

    private void chooseReminderTime(long calendarId, CalendarReminderWriter.ReminderType type) {
        boolean ticketSale = type == CalendarReminderWriter.ReminderType.TICKET_SALE;
        int[] minutes = ticketSale ? new int[]{0, 5, 30, 1440} : new int[]{0, 120, 1440};
        String[] labels = ticketSale
                ? new String[]{getString(R.string.calendar_at_start),
                getString(R.string.ticket_sale_five_minutes_before),
                getString(R.string.ticket_sale_thirty_minutes_before),
                getString(R.string.calendar_one_day_before)}
                : new String[]{getString(R.string.calendar_at_start),
                getString(R.string.calendar_two_hours_before), getString(R.string.calendar_one_day_before)};
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.calendar_remind_when)
                .setItems(labels, (dialog, which) -> saveCalendarReminder(calendarId, minutes[which], type))
                .show();
    }

    private void saveCalendarReminder(long calendarId, int minutesBefore,
                                      CalendarReminderWriter.ReminderType type) {
        ConcertEntity concert = currentConcert;
        if (concert == null) return;
        android.content.Context appContext = requireContext().getApplicationContext();
        boolean ticketSale = type == CalendarReminderWriter.ReminderType.TICKET_SALE;
        com.google.android.material.button.MaterialButton button = ticketSale
                ? binding.btnTicketSaleReminder : binding.btnCalendarReminder;
        button.setEnabled(false);
        calendarExecutor.execute(() -> {
            try {
                CalendarReminderWriter.SaveResult result = CalendarReminderWriter.save(
                        appContext, concert, calendarId, minutesBefore, type);
                mainHandler.post(() -> {
                    if (binding == null || currentConcert == null
                            || currentConcert.getId() != concert.getId()) return;
                    com.google.android.material.button.MaterialButton currentButton = ticketSale
                            ? binding.btnTicketSaleReminder : binding.btnCalendarReminder;
                    currentButton.setEnabled(true);
                    currentButton.setText(ticketSale ? R.string.ticket_sale_reminder_update
                            : R.string.calendar_reminder_update);
                    Toast.makeText(requireContext(), ticketSale
                            ? (result.updated ? R.string.ticket_sale_reminder_updated : R.string.ticket_sale_reminder_saved)
                            : (result.updated ? R.string.calendar_updated : R.string.calendar_saved),
                            Toast.LENGTH_SHORT).show();
                });
            } catch (RuntimeException e) {
                Log.w(TAG, "Unable to save calendar reminder", e);
                mainHandler.post(() -> {
                    if (binding == null || currentConcert == null
                            || currentConcert.getId() != concert.getId()) return;
                    (ticketSale ? binding.btnTicketSaleReminder : binding.btnCalendarReminder)
                            .setEnabled(true);
                    Toast.makeText(requireContext(), R.string.calendar_save_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void offerManualCalendar(int message, CalendarReminderWriter.ReminderType type) {
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.calendar_manual, (dialog, which) -> openCalendarInsert(type))
                .show();
    }

    private void openCalendarInsert(CalendarReminderWriter.ReminderType type) {
        ConcertEntity concert = currentConcert;
        boolean ticketSale = type == CalendarReminderWriter.ReminderType.TICKET_SALE;
        if (concert == null || (ticketSale
                ? concert.getTicketSaleDate() == null || concert.getTicketSaleTime() == null
                : concert.getDate() == null || concert.getTime() == null)) return;
        long start = (ticketSale ? concert.getTicketSaleDate().atTime(concert.getTicketSaleTime())
                : concert.getDate().atTime(concert.getTime())).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
        Integer duration = concert.getDurationMinutes();
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(Events.TITLE, ticketSale ? concert.getName() + " 开票" : concert.getName())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,
                        start + (ticketSale ? 15 : duration != null && duration > 0 ? duration : 120) * 60_000L)
                .putExtra(Events.EVENT_LOCATION, ticketSale ? "" : ((concert.getCity() == null ? "" : concert.getCity())
                        + " " + (concert.getVenue() == null ? "" : concert.getVenue())).trim());
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.calendar_no_writable, Toast.LENGTH_LONG).show();
        }
    }

    private void addSeatPart(List<String> parts, String value, String suffix) {
        if (value != null && !value.trim().isEmpty()) {
            parts.add(value + suffix);
        }
    }

    private String formatDuration(int totalMinutes) {
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + "分钟";
        }
        if (minutes == 0) {
            return hours + "小时";
        }
        return hours + "小时" + minutes + "分钟";
    }

    @Override
    public void onDestroyView() {
        Glide.with(this).clear(binding.imgCover);
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        calendarExecutor.shutdown();
        super.onDestroy();
    }

    private static class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {
        private List<ConcertSongEntity> songs = new ArrayList<>();

        void submitList(List<ConcertSongEntity> songs) {
            this.songs = songs;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View row = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_song_detail, parent, false);
            return new SongViewHolder(row);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            ConcertSongEntity song = songs.get(position);
            holder.number.setText(String.valueOf(position + 1));
            holder.name.setText(song.getName());
            holder.encore.setVisibility(song.isEncore() ? View.VISIBLE : View.GONE);
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        private static class SongViewHolder extends RecyclerView.ViewHolder {
            final TextView number;
            final TextView name;
            final Chip encore;

            SongViewHolder(@NonNull View row) {
                super(row);
                number = row.findViewById(R.id.tvSongNumber);
                name = row.findViewById(R.id.tvSongName);
                encore = row.findViewById(R.id.chipEncore);
            }
        }
    }

}
