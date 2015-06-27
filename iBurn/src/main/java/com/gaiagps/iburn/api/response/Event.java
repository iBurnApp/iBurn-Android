package com.gaiagps.iburn.api.response;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents an Event in the PlayaEvents JSON format
 * Created by davidbrodsky on 6/26/15.
 */
public class Event extends PlayaItem {

    // Events JSON uses "title" instead of "name"
    public String title;

    public boolean allDay;
    public boolean checkLocation;
    public EventType eventType;

    public CampRelation hostedByCamp;

    public List<EventOccurrence> occurrenceSet;

}
