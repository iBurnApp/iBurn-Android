package com.gaiagps.iburn.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gaiagps.iburn.R;

/**
 * A ListView header presenting sorting options of "Name", "Distance", and "Favorite".
 *
 * Clients register for feedback with
 * {@link #setReceiver(com.gaiagps.iburn.view.PlayaListViewHeader.PlayaListViewHeaderReceiver)}
 *
 * Created by davidbrodsky on 8/2/14.
 */
public class PlayaListViewHeader extends RelativeLayout {

    protected TextView mName;
    protected TextView mDistance;
    protected TextView mFavorite;

    private PlayaListViewHeaderReceiver mReceiver;

    public PlayaListViewHeader(Context context) {
        super(context);
        init(context);
    }

    public PlayaListViewHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PlayaListViewHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /** Interface for users to receive feedback from this view */
    public static interface PlayaListViewHeaderReceiver {
        public static enum SORT { NAME, DISTANCE, FAVORITE }

        public void onSelectionChanged(SORT sort);
    }

    /** Click listener for Sort buttons */
    protected OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!v.isSelected()) {
                v.setSelected(true);
                if (v.getTag() instanceof PlayaListViewHeaderReceiver.SORT) {
                    if (mName != null)      mName.setSelected(false);
                    if (mDistance != null)  mDistance.setSelected(false);
                    if (mFavorite != null)  mFavorite.setSelected(false);
                    v.setSelected(true);
                    dispatchSelection((PlayaListViewHeaderReceiver.SORT) v.getTag());
                }
            }
        }
    };

    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_three, this, false);
        mName       = (TextView) v.findViewById(R.id.name);
        mDistance   = (TextView) v.findViewById(R.id.distance);
        mFavorite   = (TextView) v.findViewById(R.id.favorites);
        mName.setSelected(true);
        setupTouchListeners();
        addView(v);
    }

    public void setReceiver(PlayaListViewHeaderReceiver receiver) {
        mReceiver = receiver;
    }

    protected void setupTouchListeners() {
        mName       .setTag(PlayaListViewHeaderReceiver.SORT.NAME);
        mDistance   .setTag(PlayaListViewHeaderReceiver.SORT.DISTANCE);
        mFavorite   .setTag(PlayaListViewHeaderReceiver.SORT.FAVORITE);

        mName       .setOnClickListener(mOnClickListener);
        mDistance   .setOnClickListener(mOnClickListener);
        mFavorite   .setOnClickListener(mOnClickListener);
    }

    protected void dispatchSelection(PlayaListViewHeaderReceiver.SORT sort) {
        if (mReceiver != null) {
            mReceiver.onSelectionChanged(sort);
        }
    }

}
