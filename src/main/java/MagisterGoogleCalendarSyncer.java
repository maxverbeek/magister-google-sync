

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import eu.magisterapp.magisterapi.Afspraak;
import eu.magisterapp.magisterapi.AfspraakList;
import eu.magisterapp.magisterapi.MagisterAPI;
import eu.magisterapp.magisterapi.Utils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class MagisterGoogleCalendarSyncer {
    /** Application name. */
    private static final String APPLICATION_NAME =
        "Magister Syncer";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/calendar-java-quickstart");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of the scopes required by this quickstart.
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/calendar-java-quickstart
     */
    private static final List<String> SCOPES =
        Arrays.asList(CalendarScopes.CALENDAR, CalendarScopes.CALENDAR_READONLY);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
            MagisterGoogleCalendarSyncer.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Calendar client service.
     * @return an authorized Calendar client service
     * @throws IOException
     */
    public static com.google.api.services.calendar.Calendar
        getCalendarService() throws IOException {
        Credential credential = authorize();
        return new com.google.api.services.calendar.Calendar.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void main(String[] args) throws IOException {
        // Build a new authorized API client service.
        // Note: Do not confuse this class with the
        //   com.google.api.services.calendar.model.Calendar class.
        com.google.api.services.calendar.Calendar service = getCalendarService();

        Scanner input = new Scanner(System.in);

        System.out.println("Vul je school in:");
        String school = input.nextLine();
        System.out.println("Vul je gebruikersnaam in:");
        String username = input.nextLine();

        // Dit geeft een nullpointer als je het in gradle gebruikt:
        // String password = new String(System.console().readPassword("Vul je wachtwoord in: "));

        System.out.println("Vul je wachtwoord in: ");
        String password = input.nextLine();

        MagisterAPI api = new MagisterAPI(school, username, password);

        String beginSchooljaar = "2016-9-5";

        DateTime monday = Utils.getStartOfWeek(beginSchooljaar);
        DateTime friday = Utils.getEndOfWeek(beginSchooljaar);

        AfspraakList afspraken = api.getAfspraken(monday, friday, true);

        System.out.println("Geef een naam voor de agenda:");
        String calendarName = input.nextLine();

        String roosterId = null;
        // Iterate through entries in calendar list
        String pageToken = null;
        do {
            CalendarList calendarList = service.calendarList().list().setPageToken(pageToken).execute();
            List<CalendarListEntry> items = calendarList.getItems();

            for (CalendarListEntry calendarListEntry : items) {
                if (calendarListEntry.getSummary().equals(calendarName)) {
                    roosterId = calendarListEntry.getId();

                    System.out.println(roosterId);

                    break;
                }
            }

            if (roosterId != null) break;

            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);

        if (roosterId != null) {
            System.out.println("Er is al een '" + calendarName + "' agenda. Verwijderen? y/n");
            String response = input.nextLine();

            if (response.length() > 0 && (response.charAt(0) == 'y' || response.charAt(0) == 'Y')) {
                service.calendarList().delete(roosterId).execute();
            }
        }

        System.out.println("Geef de locatie voor de nieuwe agenda:");
        String location = input.nextLine();

        // Create a new calendar list entry
        Calendar calendar = new Calendar();
        calendar.setSummary(calendarName);
        calendar.setTimeZone("Europe/Amsterdam");
        if (! location.isEmpty()) calendar.setLocation(location);

        // Insert the new calendar list entry
        Calendar roosterCalendar = service.calendars().insert(calendar).execute();

        // Zet lessen voor 1 week (herhalend elke week) in de rooster calender.

        for (Afspraak afspraak : afspraken) {
            Event event = getEventFromAfspraak(afspraak);

            service.events().insert(roosterCalendar.getId(), event).execute();
        }

        System.out.println("Done.");

    }

    private static Event getEventFromAfspraak(Afspraak afspraak) {
        Event event = new Event();

        event.setSummary(afspraak.getVak());
        event.setLocation(afspraak.getLokalen());
        event.setStart(new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(afspraak.Start.toDate()))
                .setTimeZone("Europe/Amsterdam")
        );

        event.setEnd(new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(afspraak.Einde.toDate()))
                .setTimeZone("Europe/Amsterdam")
        );

        String[] recurrence = new String[] {"RRULE:FREQ=WEEKLY;COUNT=7"}; // TODO: fix aantal keer dat hij er in moet staan (defualt 7)
        event.setRecurrence(Arrays.asList(recurrence));

        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("popup").setMinutes(2),
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        return event;
    }

}