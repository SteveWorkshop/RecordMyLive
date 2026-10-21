package rachel.materialapps.recordmylive.ui.pages.home;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract.Calendars;
import android.provider.CalendarContract.Events;
import android.provider.CalendarContract.Reminders;

import java.time.ZoneId;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import rachel.materialapps.recordmylive.logic.entity.ConcertEntity;

final class CalendarReminderWriter {
    private static final String PREFS = "concert_calendar_reminders";
    private static final String PREFERRED_CALENDAR = "preferred_calendar";

    enum ReminderType { CONCERT, TICKET_SALE }

    static final class CalendarOption {
        final long id;
        final String label;

        CalendarOption(long id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    static final class SaveResult {
        final boolean updated;

        SaveResult(boolean updated) {
            this.updated = updated;
        }
    }

    private CalendarReminderWriter() { }

    static boolean hasSavedReminder(Context context, long concertId) {
        return hasSavedReminder(context, concertId, ReminderType.CONCERT);
    }

    static boolean hasSavedReminder(Context context, long concertId, ReminderType type) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .contains(eventKey(concertId, type));
    }

    static long preferredCalendarId(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(PREFERRED_CALENDAR, -1);
    }

    static List<CalendarOption> writableCalendars(Context context) {
        List<CalendarOption> calendars = new ArrayList<>();
        String[] projection = {Calendars._ID, Calendars.CALENDAR_DISPLAY_NAME, Calendars.ACCOUNT_NAME};
        try (Cursor cursor = context.getContentResolver().query(Calendars.CONTENT_URI,
                projection, Calendars.CALENDAR_ACCESS_LEVEL + ">=?",
                new String[]{String.valueOf(Calendars.CAL_ACCESS_CONTRIBUTOR)}, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String name = cursor.getString(1);
                    String account = cursor.getString(2);
                    calendars.add(new CalendarOption(cursor.getLong(0),
                            (name == null ? "日历" : name) + " · " + (account == null ? "这台电脑" : account)));
                }
            }
        }
        return calendars;
    }

    static SaveResult save(Context context, ConcertEntity concert, long calendarId, int minutesBefore) {
        return save(context, concert, calendarId, minutesBefore, ReminderType.CONCERT);
    }

    static SaveResult save(Context context, ConcertEntity concert, long calendarId,
                           int minutesBefore, ReminderType type) {
        ContentResolver resolver = context.getContentResolver();
        String eventKey = eventKey(concert.getId(), type);
        String reminderKey = reminderKey(concert.getId(), type);
        var preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long eventId = preferences.getLong(eventKey, -1);
        long reminderId = preferences.getLong(reminderKey, -1);
        boolean updating = eventId != -1 && exists(resolver, Events.CONTENT_URI, eventId);

        ZoneId zone = ZoneId.systemDefault();
        LocalDate date = type == ReminderType.TICKET_SALE
                ? concert.getTicketSaleDate() : concert.getDate();
        LocalTime time = type == ReminderType.TICKET_SALE
                ? concert.getTicketSaleTime() : concert.getTime();
        if (date == null || time == null) {
            throw new IllegalArgumentException("Reminder requires a date and time");
        }
        long start = date.atTime(time).atZone(zone).toInstant().toEpochMilli();
        Integer duration = concert.getDurationMinutes();
        long end = start + (type == ReminderType.TICKET_SALE ? 15
                : duration != null && duration > 0 ? duration : 120) * 60_000L;
        ContentValues event = new ContentValues();
        event.put(Events.CALENDAR_ID, calendarId);
        event.put(Events.TITLE, type == ReminderType.TICKET_SALE
                ? concert.getName() + " 开票" : concert.getName());
        event.put(Events.DTSTART, start);
        event.put(Events.DTEND, end);
        event.put(Events.EVENT_TIMEZONE, zone.getId());
        String city = concert.getCity() == null ? "" : concert.getCity().trim();
        String venue = concert.getVenue() == null ? "" : concert.getVenue().trim();
        event.put(Events.EVENT_LOCATION, type == ReminderType.TICKET_SALE
                ? "" : (city + " " + venue).trim());

        if (updating) {
            int count = resolver.update(ContentUris.withAppendedId(Events.CONTENT_URI, eventId), event, null, null);
            if (count != 1) {
                throw new IllegalStateException("Calendar event could not be updated");
            }
        } else {
            Uri inserted = resolver.insert(Events.CONTENT_URI, event);
            if (inserted == null) {
                throw new IllegalStateException("Calendar event could not be inserted");
            }
            eventId = ContentUris.parseId(inserted);
            reminderId = -1;
            // Store the event first so a failed reminder write can be retried without duplicating it.
            preferences.edit().putLong(eventKey, eventId).remove(reminderKey).apply();
        }

        ContentValues reminder = new ContentValues();
        reminder.put(Reminders.EVENT_ID, eventId);
        reminder.put(Reminders.MINUTES, minutesBefore);
        reminder.put(Reminders.METHOD, Reminders.METHOD_ALERT);
        if (reminderId != -1 && exists(resolver, Reminders.CONTENT_URI, reminderId)) {
            int count = resolver.update(ContentUris.withAppendedId(Reminders.CONTENT_URI, reminderId),
                    reminder, null, null);
            if (count != 1) {
                throw new IllegalStateException("Calendar reminder could not be updated");
            }
        } else {
            Uri inserted = resolver.insert(Reminders.CONTENT_URI, reminder);
            if (inserted == null) {
                throw new IllegalStateException("Calendar reminder could not be inserted");
            }
            reminderId = ContentUris.parseId(inserted);
        }
        preferences.edit().putLong(reminderKey, reminderId)
                .putLong(PREFERRED_CALENDAR, calendarId).apply();
        return new SaveResult(updating);
    }

    static String eventKey(long concertId, ReminderType type) {
        return (type == ReminderType.TICKET_SALE ? "ticket_sale_event_" : "event_") + concertId;
    }

    static String reminderKey(long concertId, ReminderType type) {
        return (type == ReminderType.TICKET_SALE ? "ticket_sale_reminder_" : "reminder_") + concertId;
    }

    private static boolean exists(ContentResolver resolver, Uri collection, long id) {
        try (Cursor cursor = resolver.query(ContentUris.withAppendedId(collection, id),
                new String[]{"_id"}, null, null, null)) {
            return cursor != null && cursor.moveToFirst();
        }
    }
}
