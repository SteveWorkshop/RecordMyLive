package rachel.materialapps.recordmylive;

import static org.junit.Assert.assertEquals;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import rachel.materialapps.recordmylive.logic.constants.ExpenseCategory;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;
import rachel.materialapps.recordmylive.logic.util.ExpenseTotals;

public class ExpenseTotalsTest {
    @Test
    public void multipleExpensesInSameCategoryAddToActualTicketCost() {
        ConcertEntity concert = concert(1, 30000);
        concert.setFaceValuePriceCents(50000);
        ExpenseTotals totals = ExpenseTotals.calculate(Collections.singletonList(concert), Arrays.asList(
                expense(1, ExpenseCategory.TRANSPORT, 20000),
                expense(1, ExpenseCategory.TRANSPORT, 3500),
                expense(1, ExpenseCategory.TRANSPORT, 18000),
                expense(1, ExpenseCategory.FOOD, 5000)));
        assertEquals(41500L, (long) totals.categories.get(ExpenseCategory.TRANSPORT));
        assertEquals(30000L, totals.ticketCents);
        assertEquals(46500L, totals.extraCents);
        assertEquals(76500L, totals.totalCents);
    }

    @Test
    public void expensesFollowSelectedConcertsRegardlessOfPaymentDate() {
        ConcertEntity concert = concert(1, 10000);
        ExpenseEntity prepaid = expense(1, ExpenseCategory.ACCOMMODATION, 25000);
        prepaid.setSpentDate(LocalDate.of(2025, 1, 1));
        ExpenseTotals totals = ExpenseTotals.calculate(Collections.singletonList(concert), Arrays.asList(
                prepaid, expense(2, ExpenseCategory.ACCOMMODATION, 80000)));
        assertEquals(35000L, totals.totalCents);
        assertEquals(25000L, (long) totals.categories.get(ExpenseCategory.ACCOMMODATION));
        assertEquals(0L, ExpenseTotals.calculate(Collections.emptyList(),
                Collections.singletonList(prepaid)).totalCents);
    }

    @Test
    public void noExpensesKeepsOriginalTicketTotal() {
        ExpenseTotals totals = ExpenseTotals.calculate(Arrays.asList(concert(1, 10000),
                concert(2, 20000)), Collections.emptyList());
        assertEquals(30000L, totals.totalCents);
        assertEquals(0L, totals.extraCents);
        assertEquals(6, totals.categories.size());
    }

    private ConcertEntity concert(long id, long paid) {
        ConcertEntity concert = ConcertEntity.createEmpty();
        concert.setId(id);
        concert.setDate(LocalDate.of(2026, 10, 21));
        concert.setPaidPriceCents(paid);
        return concert;
    }

    private ExpenseEntity expense(long concertId, ExpenseCategory category, long cents) {
        ExpenseEntity expense = new ExpenseEntity();
        expense.setConcertId(concertId);
        expense.setCategory(category.name());
        expense.setAmountCents(cents);
        return expense;
    }
}
