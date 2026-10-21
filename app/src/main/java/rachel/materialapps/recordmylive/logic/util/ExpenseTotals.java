package rachel.materialapps.recordmylive.logic.util;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import rachel.materialapps.recordmylive.logic.constants.ExpenseCategory;
import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;
import rachel.materialapps.recordmylive.logic.entity.ExpenseEntity;

public final class ExpenseTotals {
    public final long ticketCents;
    public final long extraCents;
    public final long totalCents;
    public final Map<ExpenseCategory, Long> categories;

    private ExpenseTotals(long tickets, long extras, Map<ExpenseCategory, Long> categories) {
        this.ticketCents = tickets;
        this.extraCents = extras;
        this.totalCents = Math.addExact(tickets, extras);
        this.categories = java.util.Collections.unmodifiableMap(categories);
    }

    /** The caller selects concerts by performance date and status; payment dates do not filter expenses. */
    public static ExpenseTotals calculate(List<ConcertEntity> concerts, List<ExpenseEntity> expenses) {
        Set<Long> ids = new HashSet<>();
        long tickets = 0;
        long extras = 0;
        Map<ExpenseCategory, Long> categories = new LinkedHashMap<>();
        for (ExpenseCategory category : ExpenseCategory.values()) categories.put(category, 0L);
        for (ConcertEntity concert : concerts) {
            if (ids.add(concert.getId())) tickets = Math.addExact(tickets, concert.getPaidPriceCents());
        }
        for (ExpenseEntity expense : expenses) {
            if (!ids.contains(expense.getConcertId())) continue;
            ExpenseCategory category = ExpenseCategory.fromKey(expense.getCategory());
            categories.put(category, Math.addExact(categories.get(category), expense.getAmountCents()));
            extras = Math.addExact(extras, expense.getAmountCents());
        }
        return new ExpenseTotals(tickets, extras, categories);
    }
}
