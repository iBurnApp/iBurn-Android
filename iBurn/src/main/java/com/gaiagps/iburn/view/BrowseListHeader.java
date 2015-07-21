package com.gaiagps.iburn.view;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gaiagps.iburn.R;

/**
 * A ListView header presenting filter options for day and type
 * <p/>
 * Clients register for feedback with
 * {@link #setReceiver(BrowseListHeader.PlayaListViewHeaderReceiver)}
 * <p/>
 * Created by davidbrodsky on 8/2/14.
 */
public class BrowseListHeader extends RelativeLayout {

    public static enum BrowseSelection { ART, CAMPS, EVENT }

    private BrowseSelectionListener listener;

    protected TextView art;
    protected TextView camp;
    protected TextView events;

    public BrowseListHeader(Context context) {
        super(context);
        init(context);
    }

    public BrowseListHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BrowseListHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setBrowseSelectionListener(BrowseSelectionListener listener) {
        this.listener = listener;
    }

    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_three, this, false);
        art    = (TextView) v.findViewById(R.id.artSelection);
        camp   = (TextView) v.findViewById(R.id.campSelection);
        events = (TextView) v.findViewById(R.id.eventSelection);
        camp.setSelected(true);
        setupTouchListeners();
        addView(v);
    }

    protected void setupTouchListeners() {

        art.setTag(BrowseSelection.ART);
        camp.setTag(BrowseSelection.CAMPS);
        events.setTag(BrowseSelection.EVENT);

        art.setOnClickListener(mOnClickListener);
        camp.setOnClickListener(mOnClickListener);
        events.setOnClickListener(mOnClickListener);
    }

    public interface BrowseSelectionListener {
        void onSelectionChanged(BrowseListHeader.BrowseSelection selection);
    }

    protected OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(@NonNull final View v) {
            boolean wasSelected = v.isSelected();
            if (!v.equals(art)) art.setSelected(false);
            if (!v.equals(camp)) camp.setSelected(false);
            if (!v.equals(events)) events.setSelected(false);

            if (!wasSelected) {
                v.setSelected(true);
                if (listener != null) listener.onSelectionChanged((BrowseSelection) v.getTag());
            }
        }
    };

}
