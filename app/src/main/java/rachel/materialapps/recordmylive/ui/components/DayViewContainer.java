package rachel.materialapps.recordmylive.ui.components;

import android.view.View;

import androidx.annotation.NonNull;

import com.google.android.material.textview.MaterialTextView;
import com.kizitonwose.calendar.core.CalendarDay;
import com.kizitonwose.calendar.view.ViewContainer;

import java.util.function.Consumer;

import rachel.materialapps.recordmylive.R;

public class DayViewContainer extends ViewContainer {

    public final MaterialTextView tvDay;
    public final View dot;
    public final View todayBg;
    public final View selectedBg;

    public CalendarDay day;

    public DayViewContainer(
            @NonNull View view,
            @NonNull Consumer<CalendarDay> clickListener
    ) {
        super(view);

        tvDay = view.findViewById(R.id.tvDay);
        dot = view.findViewById(R.id.viewDot);
        todayBg = view.findViewById(R.id.viewTodayBg);
        selectedBg = view.findViewById(R.id.viewSelectedBg);

        view.setOnClickListener(v -> {
            if (day != null) {
                clickListener.accept(day);
            }
        });
    }
}