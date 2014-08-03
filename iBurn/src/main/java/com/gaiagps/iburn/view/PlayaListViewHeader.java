package com.gaiagps.iburn.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

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

    View mName;
    View mDistance;
    View mFavorite;

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
    private OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!v.isSelected()) {
                v.setSelected(true);
                if (v.getTag() instanceof PlayaListViewHeaderReceiver.SORT) {
                    mName.setSelected(false);
                    mDistance.setSelected(false);
                    mFavorite.setSelected(false);
                    v.setSelected(true);
                    dispatchSelection((PlayaListViewHeaderReceiver.SORT) v.getTag());
                }
            }
        }
    };

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header, this, false);
        mName       = v.findViewById(R.id.name);
        mDistance   = v.findViewById(R.id.distance);
        mFavorite   = v.findViewById(R.id.favorites);
        mName.setSelected(true);
        setupTouchListeners();
        addView(v);
    }

    public void setReceiver(PlayaListViewHeaderReceiver receiver) {
        mReceiver = receiver;
    }

    private void setupTouchListeners() {
        mName       .setTag(PlayaListViewHeaderReceiver.SORT.NAME);
        mDistance   .setTag(PlayaListViewHeaderReceiver.SORT.DISTANCE);
        mFavorite   .setTag(PlayaListViewHeaderReceiver.SORT.FAVORITE);

        mName       .setOnClickListener(mOnClickListener);
        mDistance   .setOnClickListener(mOnClickListener);
        mFavorite   .setOnClickListener(mOnClickListener);
    }

    private void dispatchSelection(PlayaListViewHeaderReceiver.SORT sort) {
        if (mReceiver != null) {
            mReceiver.onSelectionChanged(sort);
        }
    }

}
