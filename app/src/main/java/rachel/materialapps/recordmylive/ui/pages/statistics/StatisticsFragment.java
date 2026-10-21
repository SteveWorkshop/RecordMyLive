package rachel.materialapps.recordmylive.ui.pages.statistics;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import rachel.materialapps.recordmylive.R;
import rachel.materialapps.recordmylive.databinding.FragmentStatisticsBinding;
import rachel.materialapps.recordmylive.logic.constants.ExpenseCategory;
import rachel.materialapps.recordmylive.logic.util.TicketPrice;

public class StatisticsFragment extends Fragment {

    private FragmentStatisticsBinding binding;
    private StatisticsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        binding.periodToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btnMonth) viewModel.setPeriod(StatisticsViewModel.Period.MONTH);
            else if (checkedId == R.id.btnYear) viewModel.setPeriod(StatisticsViewModel.Period.YEAR);
            else if (checkedId == R.id.btnAll) viewModel.setPeriod(StatisticsViewModel.Period.ALL);
        });
        binding.btnPrevious.setOnClickListener(v -> viewModel.movePeriod(-1));
        binding.btnNext.setOnClickListener(v -> viewModel.movePeriod(1));

        viewModel.getPeriod().observe(getViewLifecycleOwner(), ignored -> updatePeriodHeader());
        viewModel.getAnchor().observe(getViewLifecycleOwner(), ignored -> updatePeriodHeader());
        viewModel.getSummary().observe(getViewLifecycleOwner(), this::renderSummary);
        binding.periodToggle.check(R.id.btnMonth);
    }

    private void updatePeriodHeader() {
        StatisticsViewModel.Period period = viewModel.getPeriod().getValue();
        YearMonth anchor = viewModel.getAnchor().getValue();
        if (period == null || anchor == null) return;
        binding.periodNavigator.setVisibility(period == StatisticsViewModel.Period.ALL
                ? View.GONE : View.VISIBLE);
        if (period == StatisticsViewModel.Period.MONTH) {
            binding.tvPeriod.setText(anchor.format(DateTimeFormatter.ofPattern("yyyy年M月")));
        } else if (period == StatisticsViewModel.Period.YEAR) {
            binding.tvPeriod.setText(anchor.getYear() + "年");
        }
    }

    private void renderSummary(StatisticsViewModel.Summary summary) {
        if (summary == null || binding == null) return;
        binding.tvConcertCount.setText(summary.concertCount + " 场");
        binding.tvCityCount.setText(summary.cityCount + " 个");
        binding.tvTotalDuration.setText(formatDuration(summary.totalDurationMinutes));
        binding.tvTotalCost.setText(String.format(
                Locale.CHINA, "¥%,.2f", summary.totalCostCents / 100.0));
        StringBuilder costs = new StringBuilder("门票（实付）  ¥")
                .append(TicketPrice.formatCentsAsYuan(summary.expenses.ticketCents));
        for (ExpenseCategory category : ExpenseCategory.values()) {
            costs.append('\n').append(category.getDisplayName()).append("  ¥")
                    .append(TicketPrice.formatCentsAsYuan(summary.expenses.categories.get(category)));
        }
        binding.tvExpenseBreakdown.setText(costs.toString());
        renderRanking(binding.artistRankingContainer, summary.topArtists);
        renderRanking(binding.cityRankingContainer, summary.topCities);
        renderRanking(binding.typeRankingContainer, summary.topTypes);
    }

    private void renderRanking(LinearLayout container, List<StatisticsViewModel.RankItem> items) {
        container.removeAllViews();
        if (items.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("暂无数据");
            empty.setPadding(0, dp(12), 0, dp(8));
            container.addView(empty);
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            StatisticsViewModel.RankItem item = items.get(i);
            LinearLayout row = new LinearLayout(requireContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(10), 0, dp(10));

            TextView name = new TextView(requireContext());
            name.setText((i + 1) + "   " + item.name);
            name.setTextSize(16);
            row.addView(name, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            TextView count = new TextView(requireContext());
            count.setText(item.count + " 场");
            count.setTextSize(14);
            row.addView(count);
            container.addView(row);
        }
    }

    private String formatDuration(long minutes) {
        long hours = minutes / 60;
        long remaining = minutes % 60;
        if (hours == 0) return remaining + " 分钟";
        if (remaining == 0) return hours + " 小时";
        return hours + "小时" + remaining + "分钟";
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
