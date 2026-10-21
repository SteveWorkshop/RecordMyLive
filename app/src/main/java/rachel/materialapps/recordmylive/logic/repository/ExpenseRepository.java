package rachel.materialapps.recordmylive.logic.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.logic.constants.ExpenseCategory;
import rachel.materialapps.recordmylive.logic.dao.ExpenseDao;
import rachel.materialapps.recordmylive.logic.db.DatabaseManager;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;

public class ExpenseRepository {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private final ExpenseDao dao;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public ExpenseRepository(Context context) {
        dao = DatabaseManager.getInstance(context).expenseDao();
    }
    public LiveData<List<ExpenseEntity>> getForConcert(long id) { return dao.getForConcert(id); }
    public LiveData<List<ExpenseEntity>> getAll() { return dao.getAll(); }

    public void save(ExpenseEntity expense, Consumer<Boolean> callback) {
        EXECUTOR.execute(() -> {
            boolean success = false;
            try {
                if (expense.getAmountCents() <= 0 || expense.getSpentDate() == null
                        || !ExpenseCategory.fromKey(expense.getCategory()).name().equals(expense.getCategory())) {
                    throw new IllegalArgumentException("Invalid expense");
                }
                success = expense.getId() == 0 ? dao.insert(expense) > 0 : dao.update(expense) == 1;
            } catch (RuntimeException e) {
                Log.e("ExpenseRepository", "保存费用失败", e);
            }
            boolean result = success;
            mainHandler.post(() -> callback.accept(result));
        });
    }
    public void delete(ExpenseEntity expense, Consumer<Boolean> callback) {
        EXECUTOR.execute(() -> {
            boolean success = false;
            try {
                success = dao.delete(expense.getId(), expense.getConcertId()) == 1;
            } catch (RuntimeException e) {
                Log.e("ExpenseRepository", "删除费用失败", e);
            }
            boolean result = success;
            mainHandler.post(() -> callback.accept(result));
        });
    }
}
