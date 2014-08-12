package com.gaiagps.iburn.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.EventCursorAdapter;
import com.gaiagps.iburn.database.EventTable;
import com.gaiagps.iburn.database.PlayaContentProvider;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.gaiagps.iburn.view.PlayaListViewHeader;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

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

    protected String getOrdering() {
        switch (mCurrentSort) {
            case FAVORITE:
                return PlayaItemTable.name + " ASC";
            case DISTANCE:
                return super.getOrdering();
            case NAME:
                // TIME
                return EventTable.startTime + " ASC";
        }
        throw new IllegalStateException("Unknown sort requested");
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void addCursorLoaderSelectionArgs(StringBuilder selection, ArrayList<String> selectionArgs) {
        if (mCurrentSort == SORT.DISTANCE || mCurrentSort == SORT.NAME) {
            // Has not ended more than 1 hr ago
            Date now = new Date();
            Calendar nowMinusOneHr = Calendar.getInstance();
            nowMinusOneHr.setTime(now);
            nowMinusOneHr.add(Calendar.HOUR, -1);
            String nowPlusOneHrStr = PlayaClient.getISOString(nowMinusOneHr.getTime());
            if(selection.length() > 0) selection.append(" AND ");
            selection.append(String.format("(%1$s > ? )", EventTable.endTime));
            selectionArgs.add(nowPlusOneHrStr);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_event_list_view, container, false);
        //super.onCreateView(inflater, container, savedInstanceState);
        mEmptyText = (TextView) v.findViewById(android.R.id.empty);
        mListView = ((ListView) v.findViewById(android.R.id.list));
        mListView.setEmptyView(mEmptyText);
        mListView.setFastScrollEnabled(true);
        mListView.setDividerHeight(10);
        ((PlayaListViewHeader) v.findViewById(R.id.header)).setReceiver(this);
        ((TextView) v.findViewById(R.id.name)).setText(getActivity().getString(R.string.tab_time));
        ((TextView) v.findViewById(R.id.distance)).setText(getActivity().getString(R.string.tab_distance));
        return v;
    }

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        mAdapter = new EventCursorAdapter(getActivity(), null);
        super.onActivityCreated(savedInstanceState);
    }

}