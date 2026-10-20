package rachel.materialapps.recordmylive.ui.pages.timeline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

public class TimeLineViewModelTest {
    @Test
    public void emptyDataHasNoMonthHeaders() {
        assertTrue(TimeLineViewModel.buildTimelineItems(null).isEmpty());
        assertTrue(TimeLineViewModel.buildTimelineItems(Collections.emptyList()).isEmpty());
    }

    @Test
    public void groupsByYearAndMonthAndPreservesConcertOrder() {
        ConcertEntity latest = concert(2026, 10, 20);
        ConcertEntity sameDay = concert(2026, 10, 20);
        latest.setTime(LocalTime.of(20, 0));
        sameDay.setTime(LocalTime.of(18, 0));
        ConcertEntity noTime = concert(2026, 10, 1);
        ConcertEntity previousMonth = concert(2026, 9, 30);
        ConcertEntity previousYear = concert(2025, 10, 31);
        List<ConcertEntity> source = Arrays.asList(
                latest, sameDay, noTime, previousMonth, previousYear);

        List<TimeLineItem> items = TimeLineViewModel.buildTimelineItems(source);

        assertEquals(8, items.size());
        assertEquals("2026年10月", header(items, 0).getTitle());
        assertEquals(YearMonth.of(2026, 10), header(items, 0).getMonth());
        assertSame(latest, concertItem(items, 1));
        assertSame(sameDay, concertItem(items, 2));
        assertSame(noTime, concertItem(items, 3));
        assertEquals("2026年9月", header(items, 4).getTitle());
        assertSame(previousMonth, concertItem(items, 5));
        assertEquals("2025年10月", header(items, 6).getTitle());
        assertSame(previousYear, concertItem(items, 7));
        assertEquals(5, source.size());
    }

    @Test
    public void rebuildingAfterDeletionRemovesEmptyMonth() {
        ConcertEntity october = concert(2026, 10, 20);
        ConcertEntity september = concert(2026, 9, 1);
        List<TimeLineItem> original = TimeLineViewModel.buildTimelineItems(
                Arrays.asList(october, september));
        List<TimeLineItem> updated = TimeLineViewModel.buildTimelineItems(
                Collections.singletonList(september));

        assertEquals(4, original.size());
        assertEquals(2, updated.size());
        assertEquals("2026年9月", header(updated, 0).getTitle());
        assertSame(september, concertItem(updated, 1));
    }

    private TimeLineItem.MonthHeader header(List<TimeLineItem> items, int index) {
        return (TimeLineItem.MonthHeader) items.get(index);
    }

    private ConcertEntity concertItem(List<TimeLineItem> items, int index) {
        return ((TimeLineItem.ConcertItem) items.get(index)).getConcert();
    }

    private ConcertEntity concert(int year, int month, int day) {
        ConcertEntity concert = ConcertEntity.createEmpty();
        concert.setDate(LocalDate.of(year, month, day));
        return concert;
    }
}
