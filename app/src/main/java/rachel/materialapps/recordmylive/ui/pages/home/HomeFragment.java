package rachel.materialapps.recordmylive.ui.pages.home;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AlertDialog;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

import kotlin.Unit;
import lombok.Getter;
import lombok.Setter;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentHomeBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.ui.components.CalendarDayBinder;
import rachel.materialapps.recordmylive.ui.components.ConcertPreviewAdapter;
import rachel.materialapps.recordmylive.ui.components.ConcertDeleteDialog;

public class HomeFragment extends Fragment {

    @Getter
    @Setter
    private NavHostFragment navHostFragment;

    @Getter
    @Setter
    private NavController navController;

    private static final String TAG = "HomeFragment";
    private HomeViewModel mViewModel;

    private FragmentHomeBinding binding;

    private CalendarDayBinder binder;

    private ConcertPreviewAdapter adapter;
    private AlertDialog deleteDialog;

    private LocalDate selectedDate = LocalDate.now();

    private Set<LocalDate> concertDates = new HashSet<>();
    private boolean concertsLoading = true;
    private List<ConcertEntity> currentConcerts = java.util.Collections.emptyList();

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding=FragmentHomeBinding.inflate(inflater,container,false);
        View view=binding.getRoot();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        navHostFragment = (NavHostFragment) requireActivity().getSupportFragmentManager()
                .findFragmentById(R.id.my_nav_host_fragment);
        navController = navHostFragment.getNavController();
        initRecyclerView();

        mViewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            selectedDate = date;
            binding.tvSelectedDate.setText(DateTimeFormatter.ofPattern("M月d日").format(date));
            binding.calendarView.notifyCalendarChanged();
        });

        mViewModel.getMonthMarks().observe(getViewLifecycleOwner(), dates -> {
            concertDates.clear();
            concertDates.addAll(dates);
            binding.calendarView.notifyCalendarChanged();
        });

        mViewModel.getConcerts().observe(getViewLifecycleOwner(), concerts -> {
            currentConcerts = concerts == null ? java.util.Collections.emptyList() : concerts;
            adapter.submitList(currentConcerts);
            updateConcertVisibility(currentConcerts);
        });

        mViewModel.getInitialLoading().observe(getViewLifecycleOwner(), loading -> {
            concertsLoading = Boolean.TRUE.equals(loading);
            updateConcertVisibility(currentConcerts);
        });

        mViewModel.getUpcomingConcerts().observe(getViewLifecycleOwner(), concerts -> {
            LocalDate today = LocalDate.now();
            String countdownText;
            if (concerts == null || concerts.isEmpty()) {
                countdownText = "暂无安排中的演出";
            } else {
                LocalDate nextDate = concerts.get(0).getDate();
                long days = ChronoUnit.DAYS.between(today, nextDate);
                if(days==0){
                    countdownText = "今天就可以见面啦，祝你玩得愉快！🎉";
                }
                else{
                    countdownText = "距离下次见面还有 " + days + " 天！🎉";
                }

            }
            binding.tvNextConcert.setText(countdownText);
        });

        navController.getCurrentBackStackEntry().getSavedStateHandle()
                .<String>getLiveData("selectedDate").observe(getViewLifecycleOwner(), date -> {
                    if (date != null) {
                        mViewModel.selectDate(LocalDate.parse(date));
                        navController.getCurrentBackStackEntry().getSavedStateHandle()
                                .remove("selectedDate");
                    }
                });

        binder = new CalendarDayBinder(
                () -> selectedDate,
                () -> concertDates,
                date -> mViewModel.selectDate(date)
        );

        binding.calendarView.setDayViewResource(R.layout.item_calendar_day);
        binding.calendarView.setDayBinder(binder);

        YearMonth currentMonth = YearMonth.now();
        binding.calendarView.setup(currentMonth.minusMonths(24), currentMonth.plusMonths(24), DayOfWeek.MONDAY);
        FragmentHomeBinding viewBinding = binding;
        binding.calendarView.setMonthScrollListener(month -> {
            // A resize can destroy this view while the calendar still has a queued scroll callback.
            if (binding != viewBinding) {
                return Unit.INSTANCE;
            }
            YearMonth ym = month.getYearMonth();
            Log.d(TAG, "onViewCreated: 获得当前年月");
            viewBinding.tvMonth.setText(DateTimeFormatter.ofPattern("yyyy年M月").format(ym.atDay(1)));
            mViewModel.setMonth(ym);
            return Unit.INSTANCE;
        });

        binding.calendarView.scrollToMonth(currentMonth);

        //fab跳转
        binding.fabAdd.setOnClickListener(v->{
            Bundle args = new Bundle();
            args.putString("selectedDate", selectedDate.toString());
            navController.navigate(R.id.addConcertFragment, args);
        });
    }

    @Override
    public void onDestroyView() {
        // Invalidate callbacks before Fragment teardown can dispatch pending layout work.
        binding = null;
        if (deleteDialog != null) {
            deleteDialog.dismiss();
            deleteDialog = null;
        }
        super.onDestroyView();
    }

    private void openDetail(ConcertEntity concert) {
        Bundle bundle = new Bundle();
        bundle.putLong("concertId", concert.getId());
        NavHostFragment.findNavController(this).navigate(R.id.detailFragment, bundle);
    }

    private void initRecyclerView() {
        adapter = new ConcertPreviewAdapter(
                this::openDetail,
                this::openEdit,
                this::confirmDelete
        );
        binding.rvConcert.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvConcert.setAdapter(adapter);
    }

    private void updateConcertVisibility(List<ConcertEntity> concerts) {
        if (binding == null) return;
        if (binding.loadingIndicator == null) return;
        binding.loadingIndicator.setVisibility(concertsLoading ? View.VISIBLE : View.GONE);
        binding.rvConcert.setVisibility(!concertsLoading && !concerts.isEmpty() ? View.VISIBLE : View.GONE);
        binding.emptyView.setVisibility(!concertsLoading && concerts.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void openEdit(ConcertEntity concert) {
        Bundle args = new Bundle();
        args.putLong("concertId", concert.getId());
        navController.navigate(R.id.addConcertFragment, args);
    }

    private void confirmDelete(ConcertEntity concert) {
        if (deleteDialog != null && deleteDialog.isShowing()) return;
        deleteDialog = ConcertDeleteDialog.show(requireContext(), concert,
                callback -> mViewModel.deleteConcert(concert, callback));
    }
}
