package com.gaiagps.iburn.json;

import android.content.ContentValues;

import com.gaiagps.iburn.database.ArtTable;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class JSONDeserializers {

    /**
     * Helper class to adapt JsonObjects to ContentValues for
     * direct database insertion.
     */
    public static class JsonToContentValuesAdapter {

        JsonObject mJson;
        ContentValues mCv;

        public JsonToContentValuesAdapter(JsonObject json, ContentValues cv) {
            init(json, cv);
        }

        public void swapObjects(JsonObject json, ContentValues cv) {
            init(json, cv);
        }

        private void init(JsonObject json, ContentValues cv) {
            mJson = json;
            mCv = cv;
        }

        public void adapt(String contentValuesKey, String jsonKey, Class type) {
            if (mJson.has(jsonKey)) {
                if (!mJson.get(jsonKey).isJsonNull()) {
                    if (type == String.class) {
                        mCv.put(contentValuesKey, mJson.get(jsonKey).getAsString());
                    } else if (type == Double.class) {
                        mCv.put(contentValuesKey, mJson.get(jsonKey).getAsDouble());
                    } else if (type == Integer.class) {
                        mCv.put(contentValuesKey, mJson.get(jsonKey).getAsInt());
                    }
                }
            }
        }

        public ContentValues getContentValues() {
            return mCv;
        }
    }

    public static class PlayaItemDeserializer implements JsonDeserializer<ContentValues> {

        private static JsonToContentValuesAdapter mAdapter;

        public ContentValues deserialize(JsonElement json, Type type,
                                         JsonDeserializationContext context) throws JsonParseException {

            ContentValues cv = new ContentValues();
            JsonObject object = json.getAsJsonObject();

            if (mAdapter == null) {
                mAdapter = new JsonToContentValuesAdapter(object, cv);
            } else {
                mAdapter.swapObjects(object, cv);
            }

            /* Json fields directly copied to ContentValues */
            mAdapter.adapt(PlayaItemTable.name, PlayaItemJSON.KEY_NAME, String.class);
            mAdapter.adapt(PlayaItemTable.description, PlayaItemJSON.KEY_DESCRIPTION, String.class);
            mAdapter.adapt(PlayaItemTable.playaId, PlayaItemJSON.KEY_PLAYA_ID, Integer.class);
            mAdapter.adapt(PlayaItemTable.contact, PlayaItemJSON.KEY_CONTACT, String.class);
            mAdapter.adapt(PlayaItemTable.latitude, PlayaItemJSON.KEY_LATITUDE, Double.class);
            mAdapter.adapt(PlayaItemTable.longitude, PlayaItemJSON.KEY_LONGITUDE, Double.class);
            mAdapter.adapt(PlayaItemTable.url, PlayaItemJSON.KEY_URL, String.class);
            mAdapter.adapt(PlayaItemTable.playaAddress, PlayaItemJSON.KEY_LOCATION, String.class);

//            /* Json fields massaged into ContentValues */
//            if (object.has(PlayaItemJSON.KEY_LOCATION)
//                    && !object.get(PlayaItemJSON.KEY_LOCATION).isJsonNull()) {
//                // TODO: Prase playa address? e.g: Alyssum & 8:30
//
//            }
            return cv;
        }

        public JsonToContentValuesAdapter getAdapter() {
            return mAdapter;
        }

    }

    /**
     * Deserializes an array of JSON objects representing Burning Man Camps
     * into an array of ContentValues for direct insertion into a database
     *
     * @author davidbrodsky
     */
    public static class CampsDeserializer implements JsonDeserializer<ArrayList<ContentValues>> {

        private static PlayaItemDeserializer mPlayaItemDeserializer = new PlayaItemDeserializer();

        public ArrayList<ContentValues> deserialize(JsonElement json, Type type,
                                                    JsonDeserializationContext context) throws JsonParseException {

            ArrayList<ContentValues> result = new ArrayList<>();

            JsonArray array = json.getAsJsonArray();
            int len = array.size();
            JsonToContentValuesAdapter adapter = null;
            for (int x = 0; x < len; x++) {
                ContentValues cv = mPlayaItemDeserializer.deserialize(array.get(x), type, context);
                // Don't fetch adapter until PlayaItemDeserializer operates on at least one item
                if (adapter == null) adapter = mPlayaItemDeserializer.getAdapter();
                try {
                    adapter.adapt(CampTable.hometown, CampJSON.KEY_HOMETOWN, String.class);
                    result.add(cv);
                } catch (Throwable t) {
                    throw new JsonParseException(t);
                }
            }

            return result;
        }
    }

    public static class EventsDeserializer implements JsonDeserializer<ArrayList<ContentValues>> {

        private static PlayaItemDeserializer mPlayaItemDeserializer = new PlayaItemDeserializer();

        public ArrayList<ContentValues> deserialize(JsonElement json, Type type,
                                                    JsonDeserializationContext context) throws JsonParseException {

            // Playa-data date input format
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            // Date print format E hh:mm
            SimpleDateFormat datePrinter = new SimpleDateFormat("EEEE h:mm a");
            // All day date print format E
            SimpleDateFormat dayPrinter = new SimpleDateFormat("EEEE");
            ArrayList<ContentValues> result = new ArrayList<>();

            JsonArray array = json.getAsJsonArray();
            int len = array.size();
            JsonToContentValuesAdapter adapter = null;
            JsonObject object = null;
            for (int x = 0; x < len; x++) {
                ContentValues cv = mPlayaItemDeserializer.deserialize(array.get(x), type, context);
                // Don't fetch adapter until PlayaItemDeserializer operates on at least one item
                if (adapter == null) adapter = mPlayaItemDeserializer.getAdapter();
                try {

                    // Events include a field "title", instead of "name" like Camps, Art
                    adapter.adapt(EventTable.name, EventJSON.KEY_NAME, String.class);
                    object = array.get(x).getAsJsonObject();
                    if (object.has(EventJSON.KEY_ALL_DAY) && !object.get(EventJSON.KEY_ALL_DAY).isJsonNull()) {
                        if (object.get(EventJSON.KEY_ALL_DAY).getAsString().trim().toLowerCase().compareTo("false") == 0)
                            cv.put(EventTable.allDay, 0);
                        else if (object.get(EventJSON.KEY_ALL_DAY).getAsString().trim().toLowerCase().compareTo("true") == 0)
                            cv.put(EventTable.allDay, 1);
                    }

                    if (object.has(EventJSON.KEY_CHECK_LOCATION) && !object.get(EventJSON.KEY_CHECK_LOCATION).isJsonNull()) {
                        if (object.get(EventJSON.KEY_CHECK_LOCATION).getAsString().trim().toLowerCase().compareTo("false") == 0)
                            cv.put(EventTable.checkLocation, 0);
                        else if (object.get(EventJSON.KEY_CHECK_LOCATION).getAsString().trim().toLowerCase().compareTo("true") == 0)
                            cv.put(EventTable.checkLocation, 1);
                    }

                    if (object.has(EventJSON.KEY_HOST_CAMP) && !object.get(EventJSON.KEY_HOST_CAMP).isJsonNull()) {
                        JsonObject camp = object.get(EventJSON.KEY_HOST_CAMP).getAsJsonObject();
                        if (camp.has(EventJSON.KEY_HOST_CAMP_NAME))
                            if (!camp.get(EventJSON.KEY_HOST_CAMP_NAME).isJsonNull())
                                cv.put(EventTable.campName, camp.get(EventJSON.KEY_HOST_CAMP_NAME).getAsString());

                        if (camp.has(EventJSON.KEY_HOST_CAMP_ID))
                            if (!camp.get(EventJSON.KEY_HOST_CAMP_ID).isJsonNull())
                                cv.put(EventTable.campPlayaId, camp.get(EventJSON.KEY_HOST_CAMP_ID).getAsString());
                    }

                    if (object.has(EventJSON.KEY_EVENT_TYPE) && !object.get(EventJSON.KEY_EVENT_TYPE).isJsonNull()) {
                        JsonObject eventType = object.get(EventJSON.KEY_EVENT_TYPE).getAsJsonObject();
                        if (eventType.has(EventJSON.KEY_EVENT_ABBREV) && !object.get(EventJSON.KEY_EVENT_TYPE).isJsonNull()) {
                            cv.put(EventTable.eventType, eventType.get(EventJSON.KEY_EVENT_ABBREV).getAsString());
                        }
                    }

                    if (object.has(EventJSON.KEY_OCCURENCE_SET)) {
                        if (!object.get(EventJSON.KEY_OCCURENCE_SET).isJsonNull()) {
                            JsonArray occurences = object.get(EventJSON.KEY_OCCURENCE_SET).getAsJsonArray();
                            JsonObject occurence;
                            for (int y = 0; y < occurences.size(); y++) {
                                occurence = (JsonObject) occurences.get(y);

                                if (occurence.has(EventJSON.KEY_OCCURENCE_START_TIME))
                                    if (!occurence.get(EventJSON.KEY_OCCURENCE_START_TIME).isJsonNull()) {
                                        cv.put(EventTable.startTime, occurence.get(EventJSON.KEY_OCCURENCE_START_TIME).getAsString());
                                        // Pre-compute the 'prettified' start date String.
                                        Date startDate = dateFormatter.parse(occurence.get(EventJSON.KEY_OCCURENCE_START_TIME).getAsString());
                                        if (cv.getAsBoolean(EventTable.allDay))
                                            cv.put(EventTable.startTimePrint, dayPrinter.format(startDate));
                                        else
                                            cv.put(EventTable.startTimePrint, datePrinter.format(startDate));
                                    }

                                if (occurence.has(EventJSON.KEY_OCCURENCE_END_TIME))
                                    if (!occurence.get(EventJSON.KEY_OCCURENCE_END_TIME).isJsonNull()) {
                                        cv.put(EventTable.endTime, occurence.get(EventJSON.KEY_OCCURENCE_END_TIME).getAsString());
                                        Date endDate = dateFormatter.parse(occurence.get(EventJSON.KEY_OCCURENCE_END_TIME).getAsString());
                                        cv.put(EventTable.endTimePrint, datePrinter.format(endDate));
                                    }

                                // Add a unique Event record for each occurrence of the event
                                result.add(cv);
                                cv = new ContentValues(cv);
                            }
                        }
                    }

                } catch (Throwable t) {
                    throw new JsonParseException(t);
                }
            }

            return result;
        }// end deserialize
    } // end EventDeserializer

    public static class ArtDeserializer implements JsonDeserializer<ArrayList<ContentValues>> {

        private static PlayaItemDeserializer mPlayaItemDeserializer = new PlayaItemDeserializer();

        public ArrayList<ContentValues> deserialize(JsonElement json, Type type,
                                                    JsonDeserializationContext context) throws JsonParseException {

            ArrayList<ContentValues> result = new ArrayList<>();

            JsonArray array = json.getAsJsonArray();
            int len = array.size();
            JsonToContentValuesAdapter adapter = null;
            for (int x = 0; x < len; x++) {
                ContentValues cv = mPlayaItemDeserializer.deserialize(array.get(x), type, context);
                // Don't fetch adapter until PlayaItemDeserializer operates on at least one item
                if (adapter == null) adapter = mPlayaItemDeserializer.getAdapter();
                try {
                    adapter.adapt(ArtTable.artist, ArtJSON.KEY_ARTIST, String.class);
                    adapter.adapt(ArtTable.artistLoc, ArtJSON.KEY_ARTIST_LOCATION, String.class);
                    // TODO: Image url different than base url?
                    result.add(cv);
                } catch (Throwable t) {
                    throw new JsonParseException(t);
                }
            }

            return result;
        }
    }

}
