package com.gaiagps.iburn.api.response;

import java.util.List;

/**
 * Represents an Event in the PlayaEvents JSON format
 * Created by davidbrodsky on 6/26/15.
 */
public class Event extends PlayaItem {

    // Events JSON uses "title" instead of "name"
    public String title;

    // JSON now provides booleans instead of 0/1 integers
    public boolean allDay;
    public boolean checkLocation;

    // unique id for the event in the PlayaEvents feed
    public int eventId;
    public EventType eventType;

    public String hostedByCamp; // playaId
    public String locatedAtArt; // playaId
    public String otherLocation;
    public String printDescription;
    public String slug;

    public List<EventOccurrence> occurrenceSet;

}
