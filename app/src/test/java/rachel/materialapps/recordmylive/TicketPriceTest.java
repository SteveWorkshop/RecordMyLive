package rachel.materialapps.recordmylive;

import org.junit.Test;

import rachel.materialapps.recordmylive.logic.util.TicketPrice;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TicketPriceTest {
    @Test
    public void parsesYuanToExactCents() {
        assertEquals(Long.valueOf(1234), TicketPrice.parseYuanToCents("12.34"));
        assertEquals(Long.valueOf(1200), TicketPrice.parseYuanToCents("12"));
        assertEquals(Long.valueOf(0), TicketPrice.parseYuanToCents(""));
    }

    @Test
    public void rejectsInvalidPrecisionNegativeAndOverflow() {
        assertNull(TicketPrice.parseYuanToCents("1.234"));
        assertNull(TicketPrice.parseYuanToCents("-0.01"));
        assertNull(TicketPrice.parseYuanToCents("999999999999999999999999"));
    }

    @Test
    public void formatsCentsWithTwoDecimalPlaces() {
        assertEquals("12.34", TicketPrice.formatCentsAsYuan(1234));
        assertEquals("0.05", TicketPrice.formatCentsAsYuan(5));
    }
}
