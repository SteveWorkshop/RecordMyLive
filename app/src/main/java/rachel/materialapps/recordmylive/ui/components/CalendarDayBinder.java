package rachel.materialapps.recordmylive.ui.components;

import android.view.View;

import androidx.annotation.NonNull;

import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.core.DayPosition;
import com.kizitonwose.calendar.view.MonthDayBinder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CalendarDayBinder implements MonthDayBinder<DayViewContainer> {

    private final Supplier<LocalDate> selectedDateProvider;
    private final Supplier<Set<LocalDate>> concertDateProvider;
    private final Consumer<LocalDate> clickListener;

    public CalendarDayBinder(
            Supplier<LocalDate> selectedDateProvider,
            Supplier<Set<LocalDate>> concertDateProvider,
            Consumer<LocalDate> clickListener
    ) {
        this.selectedDateProvider = selectedDateProvider;
        this.concertDateProvider = concertDateProvider;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public DayViewContainer create(@NonNull View view) {

        return new DayViewContainer(view, day -> {

            if (clickListener != null) {
                clickListener.accept(day.getDate());
            }

        });
    }

    @Override
    public void bind(
            @NonNull DayViewContainer container,
            @NonNull CalendarDay day
    ) {

        container.day = day;

        LocalDate date = day.getDate();

        LocalDate selectedDate = selectedDateProvider.get();

        Set<LocalDate> concertDates = concertDateProvider.get();

        container.tvDay.setText(String.valueOf(date.getDayOfMonth()));

        container.todayBg.setVisibility(
                date.equals(LocalDate.now()) ? View.VISIBLE : View.GONE
        );

        container.selectedBg.setVisibility(
                date.equals(selectedDate) ? View.VISIBLE : View.GONE
        );

        container.dot.setVisibility(
                concertDates.contains(date) ? View.VISIBLE : View.GONE
        );

        container.getView().setAlpha(
                day.getPosition() == DayPosition.MonthDate ? 1f : 0.35f
        );
    }
}