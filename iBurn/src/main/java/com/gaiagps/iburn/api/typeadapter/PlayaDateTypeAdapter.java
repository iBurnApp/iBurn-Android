package com.gaiagps.iburn.api.typeadapter;

import com.gaiagps.iburn.DateUtil;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * A ever-so-slightly modified version of {@link com.google.gson.internal.bind.DateTypeAdapter}
 * that works with the PlayaEventsAPI Date format e.g: "2016-09-01T21:00:00-7:00"
 */
public final class PlayaDateTypeAdapter extends TypeAdapter<Date> {
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == Date.class ? (TypeAdapter<T>) new PlayaDateTypeAdapter() : null;
        }
    };

    private final DateFormat iso8601Format = buildIso8601Format();

    // Useful when generating date strings for use in database queries of
    // event start and end times
    public static DateFormat buildIso8601Format() {
        return DateUtil.getPlayaTimeFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    @Override public Date read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        return deserializeToDate(in.nextString());
    }

    private synchronized Date deserializeToDate(String json) {
        try {
            return iso8601Format.parse(json);
        } catch (Exception e) {
            Timber.e(e, "Unable to parse date %s", json);
            throw new JsonSyntaxException(json, e);
        }
    }

    @Override public synchronized void write(JsonWriter out, Date value) throws IOException {
        if (value == null) {
            out.nullValue();
            return;
        }
        String dateFormatAsString = iso8601Format.format(value);
        out.value(dateFormatAsString);
    }
}