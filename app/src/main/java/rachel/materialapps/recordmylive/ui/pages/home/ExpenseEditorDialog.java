package rachel.materialapps.recordmylive.ui.pages.home;

import android.app.Dialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ScrollView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import rachel.materialapps.recordmylive.logic.constants.ExpenseCategory;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.util.TicketPrice;

public class ExpenseEditorDialog extends DialogFragment {
    private static final String DATE_PICKER_TAG = "expenseDatePicker";
    private MaterialAutoCompleteTextView category;
    private TextInputEditText amount;
    private TextInputEditText date;
    private TextInputEditText note;

    public static ExpenseEditorDialog create(long concertId, @Nullable ExpenseEntity expense) {
        ExpenseEditorDialog dialog = new ExpenseEditorDialog();
        Bundle args = new Bundle();
        args.putLong("concertId", concertId);
        args.putLong("id", expense == null ? 0 : expense.getId());
        args.putString("category", expense == null ? ExpenseCategory.TRANSPORT.name() : expense.getCategory());
        args.putString("amount", expense == null ? "" : TicketPrice.formatCentsAsYuan(expense.getAmountCents()));
        args.putString("date", (expense == null ? LocalDate.now() : expense.getSpentDate()).toString());
        args.putString("note", expense == null ? "" : expense.getNote());
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle values = savedInstanceState == null ? requireArguments() : savedInstanceState;
        LinearLayout form = new LinearLayout(requireContext());
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        form.setPadding(padding, padding / 2, padding, 0);
        String[] labels = new String[ExpenseCategory.values().length];
        for (int i = 0; i < labels.length; i++) labels[i] = ExpenseCategory.values()[i].getDisplayName();
        TextInputLayout categoryLayout = new TextInputLayout(requireContext());
        categoryLayout.setHint("费用分类");
        categoryLayout.setEndIconMode(TextInputLayout.END_ICON_DROPDOWN_MENU);
        category = new MaterialAutoCompleteTextView(requireContext());
        category.setInputType(InputType.TYPE_NULL);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, labels);
        category.setAdapter(adapter);
        category.setText(labels[ExpenseCategory.fromKey(values.getString("category")).ordinal()], false);
        category.setContentDescription("费用分类");
        categoryLayout.addView(category, new LinearLayout.LayoutParams(-1, -2));
        form.addView(categoryLayout, new LinearLayout.LayoutParams(-1, -2));
        amount = addField(form, "金额（元）", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        date = addField(form, "消费日期", InputType.TYPE_NULL);
        date.setFocusable(false);
        date.setClickable(true);
        date.setOnClickListener(v -> showDatePicker());
        note = addField(form, "名称或备注（选填）", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        amount.setText(values.getString("amount", ""));
        date.setText(values.getString("date", LocalDate.now().toString()));
        note.setText(values.getString("note", ""));
        ScrollView scroll = new ScrollView(requireContext());
        scroll.addView(form);
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(requireArguments().getLong("id") == 0 ? "添加费用" : "编辑费用")
                .setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("保存", null).create();
    }

    private TextInputEditText addField(LinearLayout form, String hint, int inputType) {
        TextInputLayout layout = new TextInputLayout(requireContext());
        layout.setHint(hint);
        TextInputEditText input = new TextInputEditText(layout.getContext());
        input.setInputType(inputType);
        input.setSingleLine(true);
        layout.addView(input, new LinearLayout.LayoutParams(-1, -2));
        form.addView(layout, new LinearLayout.LayoutParams(-1, -2));
        return input;
    }

    @Override
    public void onStart() {
        super.onStart();
        AlertDialog dialog = (AlertDialog) requireDialog();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> save(dialog));
        DialogFragment restoredPicker = (DialogFragment) getChildFragmentManager()
                .findFragmentByTag(DATE_PICKER_TAG);
        if (restoredPicker instanceof MaterialDatePicker) {
            bindDatePicker((MaterialDatePicker<?>) restoredPicker);
        }
    }

    private void showDatePicker() {
        if (getChildFragmentManager().findFragmentByTag(DATE_PICKER_TAG) != null) return;
        LocalDate selectedDate = LocalDate.parse(date.getText().toString());
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("选择消费日期")
                .setSelection(selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                .build();
        bindDatePicker(picker);
        picker.show(getChildFragmentManager(), DATE_PICKER_TAG);
    }

    private void bindDatePicker(MaterialDatePicker<?> picker) {
        picker.clearOnPositiveButtonClickListeners();
        picker.addOnPositiveButtonClickListener(selection -> {
            LocalDate selectedDate = Instant.ofEpochMilli((Long) selection)
                    .atZone(ZoneOffset.UTC).toLocalDate();
            date.setText(selectedDate.toString());
            date.setError(null);
        });
    }

    private void save(AlertDialog dialog) {
        Long cents = TicketPrice.parseYuanToCents(amount.getText().toString());
        if (cents == null || cents <= 0) {
            amount.setError("请输入大于 0 的金额，最多两位小数");
            return;
        }
        LocalDate spentDate;
        try {
            spentDate = LocalDate.parse(date.getText().toString().trim());
        } catch (DateTimeParseException e) {
            date.setError("请选择消费日期");
            return;
        }
        ExpenseEntity expense = new ExpenseEntity();
        expense.setId(requireArguments().getLong("id"));
        expense.setConcertId(requireArguments().getLong("concertId"));
        expense.setCategory(ExpenseCategory.values()[getCategoryIndex()].name());
        expense.setAmountCents(cents);
        expense.setSpentDate(spentDate);
        expense.setNote(note.getText().toString().trim());
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        new ViewModelProvider(requireParentFragment()).get(DetailViewModel.class)
                .saveExpense(expense, success -> {
                    if (!isAdded()) return;
                    if (success) dismissAllowingStateLoss();
                    else {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        Toast.makeText(requireContext(), "保存失败，请重试", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (category == null) return;
        outState.putString("category", ExpenseCategory.values()[getCategoryIndex()].name());
        outState.putString("amount", amount.getText().toString());
        outState.putString("date", date.getText().toString());
        outState.putString("note", note.getText().toString());
    }

    private int getCategoryIndex() {
        String selected = category.getText().toString();
        for (int i = 0; i < ExpenseCategory.values().length; i++) {
            if (ExpenseCategory.values()[i].getDisplayName().equals(selected)) return i;
        }
        return 0;
    }
}
