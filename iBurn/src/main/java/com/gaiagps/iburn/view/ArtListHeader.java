package com.gaiagps.iburn.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gaiagps.iburn.R;

/**
 * A ListView header presenting filter options for Art featured in the audio tour
 * <p>
 * Clients register for feedback with
 * {@link #setListener(Listener)}
 * <p>
 * Created by davidbrodsky on 8/2/14.
 */
public class ArtListHeader extends RelativeLayout {

    protected TextView mAllArt;
    protected TextView mAudioTourArt;

    protected Listener mListener;

    // Don't use selected state, because we don't want the selected appearance
    private boolean mShowAudioTourOnly;

    public ArtListHeader(Context context) {
        super(context);
        init(context);
    }

    public ArtListHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ArtListHeader(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    /**
     * Interface for users to receive feedback from this view
     */
    public interface Listener {
        void onSelectionChanged(boolean showAudioTourOnly);
    }

    /**
     * Clicklistener for Sort buttons
     */
    protected OnClickListener mOnClickListener = view -> {
        mShowAudioTourOnly = !mShowAudioTourOnly;

        if (mShowAudioTourOnly) {
            mAudioTourArt.setSelected(true);
            mAllArt.setSelected(false);
        } else {
            mAudioTourArt.setSelected(false);
            mAllArt.setSelected(true);
        }
        dispatchSelection(mShowAudioTourOnly);
    };

    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_two_art, this, false);
        mAllArt = (TextView) v.findViewById(R.id.typeFilter);
        mAllArt.setText(R.string.all_art);
        mAllArt.setSelected(true);
        mAudioTourArt = (TextView) v.findViewById(R.id.dateFilter);
        mAudioTourArt.setText(R.string.audio_tour_art_only);
        setupTouchListeners();
        addView(v);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    protected void setupTouchListeners() {
        mAllArt.setOnClickListener(mOnClickListener);
        mAudioTourArt.setOnClickListener(mOnClickListener);
    }

    protected void dispatchSelection(boolean showAudioTourOnly) {
        if (mListener != null) {
            mListener.onSelectionChanged(showAudioTourOnly);
        }
    }

}
