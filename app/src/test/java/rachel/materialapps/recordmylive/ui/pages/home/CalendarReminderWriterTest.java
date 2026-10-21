package rachel.materialapps.recordmylive.ui.pages.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

public class CalendarReminderWriterTest {
    @Test
    public void ticketSaleReminderUsesSeparateKeysFromExistingConcertReminder() {
        long concertId = 42;
        assertEquals("event_42", CalendarReminderWriter.eventKey(
                concertId, CalendarReminderWriter.ReminderType.CONCERT));
        assertEquals("reminder_42", CalendarReminderWriter.reminderKey(
                concertId, CalendarReminderWriter.ReminderType.CONCERT));
        assertEquals("ticket_sale_event_42", CalendarReminderWriter.eventKey(
                concertId, CalendarReminderWriter.ReminderType.TICKET_SALE));
        assertEquals("ticket_sale_reminder_42", CalendarReminderWriter.reminderKey(
                concertId, CalendarReminderWriter.ReminderType.TICKET_SALE));
        assertNotEquals(CalendarReminderWriter.eventKey(concertId,
                CalendarReminderWriter.ReminderType.CONCERT), CalendarReminderWriter.eventKey(
                concertId, CalendarReminderWriter.ReminderType.TICKET_SALE));
        assertNotEquals(CalendarReminderWriter.reminderKey(concertId,
                CalendarReminderWriter.ReminderType.CONCERT), CalendarReminderWriter.reminderKey(
                concertId, CalendarReminderWriter.ReminderType.TICKET_SALE));
    }
}
