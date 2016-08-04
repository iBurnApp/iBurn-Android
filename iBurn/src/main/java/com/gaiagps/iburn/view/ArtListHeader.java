package com.gaiagps.iburn.view;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.adapters.AdapterUtils;

import java.util.ArrayList;

/**
 * A ListView header presenting filter options for Art featured in the audio tour
 * <p>
 * Clients register for feedback with
 * {@link #setListener(Listener)}
 * <p>
 * Created by davidbrodsky on 8/2/14.
 */
public class ArtListHeader extends RelativeLayout {

    protected TextView mTextView;

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
            ((TextView) view).setText(R.string.audio_tour_art_only);
        } else {
            ((TextView) view).setText(R.string.all_art);
        }
        dispatchSelection(mShowAudioTourOnly);
    };

    protected void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.list_view_header_one, this, false);
        mTextView = (TextView) v.findViewById(R.id.filter);
        mTextView.setText(R.string.all_art);
        setupTouchListeners();
        addView(v);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }

    protected void setupTouchListeners() {
        mTextView.setOnClickListener(mOnClickListener);
    }

    protected void dispatchSelection(boolean showAudioTourOnly) {
        if (mListener != null) {
            mListener.onSelectionChanged(showAudioTourOnly);
        }
    }

}
