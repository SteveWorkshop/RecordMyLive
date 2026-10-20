package rachel.materialapps.recordmylive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.time.LocalDate;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

public class ConcertDateTest {
    @Test
    public void dateCannotBeSetToNull() {
        ConcertEntity concert = ConcertEntity.createEmpty();
        LocalDate date = LocalDate.of(2026, 10, 20);
        concert.setDate(date);
        assertThrows(NullPointerException.class, () -> concert.setDate(null));
        assertEquals(date, concert.getDate());
    }

    @Test
    public void draftMustHaveDateBeforeSavingButTimeIsOptional() {
        ConcertEntity concert = ConcertEntity.createEmpty();
        assertThrows(NullPointerException.class, concert::requireDate);
        concert.setDate(LocalDate.of(2026, 10, 20));
        concert.requireDate();
        concert.setTime(null);
        assertNull(concert.getTime());
    }
}
