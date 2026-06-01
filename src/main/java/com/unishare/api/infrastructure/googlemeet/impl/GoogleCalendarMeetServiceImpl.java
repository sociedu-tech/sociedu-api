package com.unishare.api.infrastructure.googlemeet.impl;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.ConferenceData;
import com.google.api.services.calendar.model.ConferenceSolutionKey;
import com.google.api.services.calendar.model.CreateConferenceRequest;
import com.google.api.services.calendar.model.EntryPoint;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.unishare.api.common.dto.AppException;
import com.unishare.api.infrastructure.googlemeet.GoogleMeetProperties;
import com.unishare.api.infrastructure.googlemeet.GoogleMeetService;
import com.unishare.api.infrastructure.googlemeet.dto.GoogleMeetCreateCommand;
import com.unishare.api.infrastructure.googlemeet.dto.GoogleMeetCreateResult;
import com.unishare.api.infrastructure.googlemeet.exception.GoogleMeetErrorCode;
import com.unishare.api.infrastructure.googlemeet.oauth.GoogleOAuthTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.google.meet", name = "enabled", havingValue = "true")
public class GoogleCalendarMeetServiceImpl implements GoogleMeetService {

    private static final String APPLICATION_NAME = "UniShare Mentoring";

    private final GoogleMeetProperties properties;
    private final GoogleOAuthTokenService oauthTokenService;

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public GoogleMeetCreateResult createMeeting(GoogleMeetCreateCommand command) {
        try {
            Credential credential = oauthTokenService.requireCredential(command.mentorUserId());
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            Calendar calendar = new Calendar.Builder(
                            transport, GsonFactory.getDefaultInstance(), credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();

            ConferenceSolutionKey key = new ConferenceSolutionKey().setType("hangoutsMeet");
            CreateConferenceRequest createRequest = new CreateConferenceRequest()
                    .setRequestId(UUID.randomUUID().toString())
                    .setConferenceSolutionKey(key);
            ConferenceData conferenceData = new ConferenceData().setCreateRequest(createRequest);

            EventDateTime start = new EventDateTime()
                    .setDateTime(toDateTime(command.scheduledAt()))
                    .setTimeZone(properties.getTimezone());
            EventDateTime end = new EventDateTime()
                    .setDateTime(toDateTime(command.scheduledAtEnd()))
                    .setTimeZone(properties.getTimezone());

            Event event = new Event()
                    .setSummary(defaultTitle(command.title()))
                    .setDescription(command.description())
                    .setStart(start)
                    .setEnd(end)
                    .setConferenceData(conferenceData);

            List<EventAttendee> attendees = buildAttendees(command.attendeeEmails());
            if (!attendees.isEmpty()) {
                event.setAttendees(attendees);
            }

            Event created = calendar
                    .events()
                    .insert(properties.getCalendarId(), event)
                    .setConferenceDataVersion(1)
                    .setSendUpdates("none")
                    .execute();

            String meetingUrl = resolveMeetingUrl(created);
            if (!StringUtils.hasText(meetingUrl)) {
                throw new AppException(
                        GoogleMeetErrorCode.CREATE_FAILED,
                        "Google Calendar không trả về link Google Meet.");
            }

            return GoogleMeetCreateResult.builder()
                    .meetingUrl(meetingUrl)
                    .calendarEventId(created.getId())
                    .calendarHtmlLink(created.getHtmlLink())
                    .build();
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to create Google Meet event mentorUserId={}", command.mentorUserId(), ex);
            throw new AppException(
                    GoogleMeetErrorCode.CREATE_FAILED,
                    "Không tạo được link Google Meet: " + ex.getMessage());
        }
    }

    private static com.google.api.client.util.DateTime toDateTime(Instant instant) {
        return new com.google.api.client.util.DateTime(Date.from(instant));
    }

    private static String defaultTitle(String title) {
        return StringUtils.hasText(title) ? title.trim() : "Buổi mentoring UniShare";
    }

    private static List<EventAttendee> buildAttendees(List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            return List.of();
        }
        List<EventAttendee> attendees = new ArrayList<>();
        for (String email : emails) {
            if (StringUtils.hasText(email)) {
                attendees.add(new EventAttendee().setEmail(email.trim()));
            }
        }
        return attendees;
    }

    private static String resolveMeetingUrl(Event created) {
        if (StringUtils.hasText(created.getHangoutLink())) {
            return created.getHangoutLink();
        }
        ConferenceData data = created.getConferenceData();
        if (data == null || data.getEntryPoints() == null) {
            return null;
        }
        for (EntryPoint entry : data.getEntryPoints()) {
            if ("video".equalsIgnoreCase(entry.getEntryPointType()) && StringUtils.hasText(entry.getUri())) {
                return entry.getUri();
            }
        }
        return null;
    }
}
