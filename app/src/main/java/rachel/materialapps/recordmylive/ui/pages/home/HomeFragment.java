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
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import kotlin.Unit;
import lombok.Getter;
import lombok.Setter;
import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentHomeBinding;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.ui.components.CalendarDayBinder;
import rachel.materialapps.recordmylive.ui.components.ConcertPreviewAdapter;

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

    private LocalDate selectedDate = LocalDate.now();

    private Set<LocalDate> concertDates = new HashSet<>();

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
            adapter.submitList(concerts);
            binding.rvConcert.setVisibility(concerts.isEmpty() ? View.GONE : View.VISIBLE);
            binding.emptyView.setVisibility(concerts.isEmpty() ? View.VISIBLE : View.GONE);
        });

        mViewModel.getUpcomingConcerts().observe(getViewLifecycleOwner(), concerts -> {
            LocalDate today = LocalDate.now();
            String countdownText;
            if (concerts == null || concerts.isEmpty()) {
                countdownText = "暂无安排中的演出";
            } else {
                LocalDate nextDate = concerts.get(0).getDate();
                long days = ChronoUnit.DAYS.between(today, nextDate);
                countdownText = "距离下次演出还有 " + days + " 天！🎉";
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
        binding.calendarView.setMonthScrollListener(month -> {
            YearMonth ym = month.getYearMonth();
            Log.d(TAG, "onViewCreated: 获得当前年月");
            binding.tvMonth.setText(DateTimeFormatter.ofPattern("yyyy年M月").format(ym.atDay(1)));
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
        super.onDestroyView();
        binding=null;
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
                concert -> {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle("删除演出")
                            .setMessage("确定删除 \"" + concert.getName() + "\" 吗？")
                            .setPositiveButton("删除", (dialog, which) -> {
                                //todo：真删
                                Toast.makeText(getContext(), "模拟删除", Toast.LENGTH_SHORT).show();
                                //mViewModel.deleteConcert(concert);
                            })
                            .setNegativeButton("取消", null)
                            .show();
                }
        );
        binding.rvConcert.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvConcert.setAdapter(adapter);
    }

    private void openEdit(ConcertEntity concert) {
        Bundle args = new Bundle();
        args.putLong("concertId", concert.getId());
        navController.navigate(R.id.addConcertFragment, args);
    }
}
