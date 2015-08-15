package com.gaiagps.iburn.api.typeadapter;

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

/**
 * A ever-so-slightly modified version of {@link com.google.gson.internal.bind.DateTypeAdapter}
 * that works with the PlayaEventsAPI Date format e.g: "2015-09-01 21:00:00"
 */
public final class PlayaDateTypeAdapter extends TypeAdapter<Date> {
    public static final TypeAdapterFactory FACTORY = new TypeAdapterFactory() {
        @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
            return typeToken.getRawType() == Date.class ? (TypeAdapter<T>) new PlayaDateTypeAdapter() : null;
        }
    };

    public static final DateFormat iso8601Format = buildIso8601Format();

    private static DateFormat buildIso8601Format() {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        iso8601Format.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        return iso8601Format;
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
        } catch (ParseException e) {
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