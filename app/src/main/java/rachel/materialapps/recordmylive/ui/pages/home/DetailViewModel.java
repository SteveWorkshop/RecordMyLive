package rachel.materialapps.recordmylive.ui.pages.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import rachel.materialapps.recordmylive.logic.entity.dto.ConcertWithSongs;
import rachel.materialapps.recordmylive.logic.repository.ConcertRepository;
import java.util.List;
import java.util.function.Consumer;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.repository.ExpenseRepository;

public class DetailViewModel extends AndroidViewModel {
    private final ConcertRepository repository;

    private final LiveData<ConcertWithSongs> detail;
    private final LiveData<List<ExpenseEntity>> expenses;
    private final ExpenseRepository expenseRepository;

    private final MutableLiveData<Long> concertId = new MutableLiveData<>();

    public DetailViewModel(@NonNull Application application) {
        super(application);
        repository = new ConcertRepository(application);
        expenseRepository = new ExpenseRepository(application);
        detail = Transformations.switchMap(
                concertId,
                repository::getConcertDetail
        );
        expenses = Transformations.switchMap(concertId, expenseRepository::getForConcert);
    }

    public LiveData<ConcertWithSongs> getDetail() {
        return detail;
    }

    public LiveData<List<ExpenseEntity>> getExpenses() { return expenses; }
    public void saveExpense(ExpenseEntity expense, Consumer<Boolean> callback) {
        expenseRepository.save(expense, callback);
    }
    public void deleteExpense(ExpenseEntity expense, Consumer<Boolean> callback) {
        expenseRepository.delete(expense, callback);
    }

    public void loadConcert(long id) {
        if (concertId.getValue() == null || concertId.getValue() != id) {
            concertId.setValue(id);
        }
    }
}
