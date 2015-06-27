package com.gaiagps.iburn.api.response;

import java.util.Date;

/**
 * Represents a single occurrence of an Event in the PlayaEvents JSON format.
 * An Event has one or more EventOcurrence
 *
 * Created by davidbrodsky on 6/26/15.
 */
public class EventOccurrence {

    public Date startTime;
    public Date endTime;
}
