package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.EventCursorAdapter;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.database.PlayaItemTable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by davidbrodsky on 8/3/13.
 * Base class for iBurn ListViews describing
 * Camps, Art, and Events. A subclass should provide
 * a value for PROJECTION, mAdapter, baseUri, and searchUri
 */
public class EventListViewFragment extends PlayaListViewFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "EventListViewFragment";

    static final String[] PROJECTION = new String[] {
            EventTable.id,
            EventTable.name,
            EventTable.startTime,
            EventTable.startTimePrint,
            EventTable.endTime,
            EventTable.endTimePrint,
            EventTable.allDay,
            EventTable.favorite,
            EventTable.latitude,
            EventTable.longitude
    };

    SimpleCursorAdapter mAdapter;
    protected Uri baseUri = PlayaContentProvider.Events.EVENTS;     // Uris corresponding to PlayaContentProvider

    public static EventListViewFragment newInstance() {
        return new EventListViewFragment();
    }

    protected Uri getBaseUri(){
        return baseUri;
    }

    protected SimpleCursorAdapter getAdapter(){
        return mAdapter;
    }

    protected String[] getProjection(){
        return PROJECTION;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void addCursorLoaderSelectionArgs(StringBuilder selection, ArrayList<String> selectionArgs) {
        if (mCurrentSort == SORT.NAME) {
            // Do a HERE + NOW
            // Starts within next hour or ends after now
            Date now = new Date();
            Calendar nowPlusOneHr = Calendar.getInstance();
            nowPlusOneHr.setTime(now);
            nowPlusOneHr.add(Calendar.HOUR, 1);
            String nowPlusOneHrStr = PlayaClient.getISOString(nowPlusOneHr.getTime());
            String nowStr = PlayaClient.getISOString(now);
            if(selection.length() > 0) selection.append(" AND ");
            selection.append(String.format("(%1$s < ? AND %1$s > ?) OR (%1$s < ? AND %2$s > ?)", EventTable.startTime, EventTable.endTime));
            selectionArgs.add(nowPlusOneHrStr);
            selectionArgs.add(nowStr);
            selectionArgs.add(nowStr);
            selectionArgs.add(nowStr);
        } else if (mCurrentSort == SORT.DISTANCE) {
            // Has not ended more than 1 hr ago
            Date now = new Date();
            Calendar nowPlusOneHr = Calendar.getInstance();
            nowPlusOneHr.setTime(now);
            nowPlusOneHr.add(Calendar.HOUR, 1);
            String nowPlusOneHrStr = PlayaClient.getISOString(nowPlusOneHr.getTime());
            if(selection.length() > 0) selection.append(" AND ");
            selection.append(String.format("(%1$s > ? )", EventTable.endTime));
            selectionArgs.add(nowPlusOneHrStr);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);
        ((TextView) v.findViewById(R.id.name)).setText(getActivity().getString(R.string.here_now));
        return v;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        mAdapter = new EventCursorAdapter(getActivity(), null);
        super.onActivityCreated(savedInstanceState);
    }

}