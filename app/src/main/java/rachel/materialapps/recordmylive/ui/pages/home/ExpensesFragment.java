package rachel.materialapps.recordmylive.ui.pages.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.databinding.FragmentExpensesBinding;
import rachel.materialapps.recordmylive.databinding.ItemExpenseBinding;
import rachel.materialapps.recordmylive.logic.constants.ExpenseCategory;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.util.ExpenseTotals;
import rachel.materialapps.recordmylive.logic.util.TicketPrice;

public class ExpensesFragment extends Fragment {
    private FragmentExpensesBinding binding;
    private DetailViewModel viewModel;
    private ConcertEntity concert;
    private List<ExpenseEntity> expenses = Collections.emptyList();
    private AlertDialog deleteDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentExpensesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
        ExpenseAdapter adapter = new ExpenseAdapter(this::showEditor, this::confirmDelete);
        binding.recyclerExpenses.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerExpenses.setAdapter(adapter);
        binding.fabAddExpense.setOnClickListener(v -> showEditor(null));
        viewModel.getDetail().observe(getViewLifecycleOwner(), detail -> {
            concert = detail == null ? null : detail.concert;
            binding.fabAddExpense.setEnabled(concert != null);
            renderSummary();
        });
        viewModel.getExpenses().observe(getViewLifecycleOwner(), items -> {
            expenses = items == null ? Collections.emptyList() : items;
            adapter.submitList(expenses);
            binding.tvEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
            binding.tvExpenseCount.setText("附加费用 · " + expenses.size() + " 笔");
            renderSummary();
        });
        viewModel.loadConcert(requireArguments().getLong("concertId"));
    }

    private void renderSummary() {
        if (concert == null) return;
        ExpenseTotals totals = ExpenseTotals.calculate(Collections.singletonList(concert), expenses);
        binding.tvConcertName.setText(concert.getName());
        binding.tvSummary.setText("门票实付 ¥" + TicketPrice.formatCentsAsYuan(totals.ticketCents)
                + "\n附加费用 ¥" + TicketPrice.formatCentsAsYuan(totals.extraCents)
                + "\n总计 ¥" + TicketPrice.formatCentsAsYuan(totals.totalCents));
    }

    private void showEditor(@Nullable ExpenseEntity expense) {
        if (concert == null || getChildFragmentManager().isStateSaved()
                || getChildFragmentManager().findFragmentByTag("expenseEditor") != null) return;
        ExpenseEditorDialog.create(concert.getId(), expense).show(getChildFragmentManager(), "expenseEditor");
    }

    private void confirmDelete(ExpenseEntity expense) {
        if (deleteDialog != null && deleteDialog.isShowing()) return;
        deleteDialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle("删除费用")
                .setMessage("删除这笔" + ExpenseCategory.fromKey(expense.getCategory()).getDisplayName()
                        + "费用 ¥" + TicketPrice.formatCentsAsYuan(expense.getAmountCents()) + "？")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("删除", (dialog, which) -> viewModel.deleteExpense(expense, success -> {
                    if (binding != null && !success) {
                        Toast.makeText(requireContext(), "删除失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                })).show();
    }

    @Override
    public void onDestroyView() {
        if (deleteDialog != null) {
            deleteDialog.dismiss();
            deleteDialog = null;
        }
        binding.recyclerExpenses.setAdapter(null);
        super.onDestroyView();
        binding = null;
    }

    private static class ExpenseAdapter extends ListAdapter<ExpenseEntity, ExpenseAdapter.Holder> {
        private final Consumer<ExpenseEntity> edit;
        private final Consumer<ExpenseEntity> delete;

        ExpenseAdapter(Consumer<ExpenseEntity> edit, Consumer<ExpenseEntity> delete) {
            super(new DiffUtil.ItemCallback<ExpenseEntity>() {
                @Override
                public boolean areItemsTheSame(@NonNull ExpenseEntity oldItem, @NonNull ExpenseEntity newItem) {
                    return oldItem.getId() == newItem.getId();
                }
                @Override
                public boolean areContentsTheSame(@NonNull ExpenseEntity oldItem, @NonNull ExpenseEntity newItem) {
                    return oldItem.getAmountCents() == newItem.getAmountCents()
                            && Objects.equals(oldItem.getCategory(), newItem.getCategory())
                            && Objects.equals(oldItem.getSpentDate(), newItem.getSpentDate())
                            && Objects.equals(oldItem.getNote(), newItem.getNote());
                }
            });
            this.edit = edit;
            this.delete = delete;
            setStateRestorationPolicy(StateRestorationPolicy.PREVENT_WHEN_EMPTY);
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemExpenseBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            ExpenseEntity expense = getItem(position);
            ItemExpenseBinding row = holder.binding;
            ExpenseCategory category = ExpenseCategory.fromKey(expense.getCategory());
            row.ivCategoryIcon.setImageResource(category.getIconResource());
            row.tvCategory.setText(category.getDisplayName());
            row.tvAmount.setText("¥" + TicketPrice.formatCentsAsYuan(expense.getAmountCents()));
            row.tvDate.setText("消费日期：" + expense.getSpentDate());
            row.tvNote.setText(expense.getNote());
            row.tvNote.setVisibility(expense.getNote().isEmpty() ? View.GONE : View.VISIBLE);
            row.btnEdit.setOnClickListener(v -> edit.accept(expense));
            row.btnDelete.setOnClickListener(v -> delete.accept(expense));
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ItemExpenseBinding binding;
            Holder(ItemExpenseBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
