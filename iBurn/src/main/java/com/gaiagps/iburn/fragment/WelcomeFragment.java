package com.gaiagps.iburn.fragment;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaDatabase;
import com.gaiagps.iburn.database.PlayaItemTable;
import com.squareup.sqlbrite.SqlBrite;

import java.io.IOException;

import timber.log.Timber;

public class WelcomeFragment extends Fragment implements TextureView.SurfaceTextureListener {

    final static String LAYOUT_ID = "layoutid";

    // Welcome 1 - Show video
    private MediaPlayer mediaPlayer;
    private TextureView textureView;

    // Welcome 3 - Set Home
    private AutoCompleteTextView campSearchView;

    public static WelcomeFragment newInstance(int layoutId) {
        WelcomeFragment pane = new WelcomeFragment();
        Bundle args = new Bundle();
        args.putInt(LAYOUT_ID, layoutId);
        pane.setArguments(args);
        return pane;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(getArguments().getInt(LAYOUT_ID, -1), container, false);

        if (getArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment1) {
            // Intro video
            textureView = ((TextureView) rootView.findViewById(R.id.video));
            textureView.setSurfaceTextureListener(this);

        } else if (getArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment3) {
            // Set Home location
            campSearchView = (AutoCompleteTextView) rootView.findViewById(R.id.campNameSearch);
            campSearchView.setAdapter(new CampAutoCompleteAdapter(getActivity()));
            campSearchView.setOnItemClickListener((parent, view, position, id) -> {
                Cursor campCursor = ((Cursor) campSearchView.getAdapter().getItem(position));
                HomeCampSelectionListener.CampSelection selection = new HomeCampSelectionListener.CampSelection(campCursor.getDouble(campCursor.getColumnIndex(CampTable.latitude)),
                        campCursor.getDouble(campCursor.getColumnIndex(CampTable.longitude)),
                        campCursor.getString(campCursor.getColumnIndex(CampTable.name)));
                campSearchView.setTag(selection);
                Timber.d("Item selected %s", campSearchView.getText().toString());

                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(campSearchView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                if (getActivity() instanceof HomeCampSelectionListener) {
                    ((HomeCampSelectionListener) getActivity()).onHomeCampSelected(selection);
                }
            });
        }
        return rootView;
    }

    @Override
    public void onDestroyView () {
        super.onDestroyView();

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Surface s = new Surface(surface);

        try {
            AssetFileDescriptor descriptor = getActivity().getAssets().openFd("mp4/onboarding_loop_final.mp4");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            mediaPlayer.setSurface(s);
            mediaPlayer.prepare();
            scaleTextureView(textureView);
            mediaPlayer.start();
            mediaPlayer.setLooping(true);

        } catch (IllegalArgumentException | SecurityException | IOException | IllegalStateException e) {
            Timber.e(e, "Error preparing video");
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //unused
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        //unused
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        //unused
    }

    private void scaleTextureView(TextureView textureView) {
        // TODO : Do this properly. We're assuming a ~9:16 portrait screen ratio
        textureView.setScaleX(1.78f);
        textureView.requestLayout();
        textureView.invalidate();

    }

    private class CampAutoCompleteAdapter extends BaseAdapter implements Filterable {

        private Cursor cursor;
        private DataProvider dataProvider;
        private CampNameFilter filter;
        LayoutInflater inflater;

        public CampAutoCompleteAdapter(Context context) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            DataProvider.getInstance(context)
                    .subscribe(readyDataProvider -> this.dataProvider = readyDataProvider);
        }

        public void changeCursor(Cursor newCursor) {
            if (cursor != null) {
                cursor.close();
                cursor = null;
            }

            cursor = newCursor;
        }

        @Override
        public int getCount() {
            return cursor == null ? 0 : cursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            if (cursor == null) return null;
            cursor.moveToPosition(position);
            return cursor;
        }

        @Override
        public long getItemId(int position) {
            if (cursor == null) return -1;
            cursor.moveToPosition(position);
            return cursor.getInt(cursor.getColumnIndex(PlayaItemTable.id));
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = new TextView(getActivity());
                convertView.setPadding(16, 16, 16, 16);
                ((TextView) convertView).setTextSize(16);
                ((TextView) convertView).setTextAppearance(getActivity(), R.style.PlayaTextItem);
            }

            if (cursor != null) {

                cursor.moveToPosition(position);

                ((TextView) convertView).setText(cursor.getString(cursor.getColumnIndex(PlayaItemTable.name)));
            }

            return convertView;
        }

        @Override
        public Filter getFilter() {

            if (filter == null) {
                filter = new CampNameFilter();
            }
            return filter;
        }

        private class CampNameFilter extends Filter {

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                Timber.d("Perform filtering with constraint %s", constraint == null ? "None" : constraint.toString());

                FilterResults r = new FilterResults();

                if (constraint != null) {
                    String query = '%' + constraint.toString() + '%';
                    Cursor newResult = dataProvider.createEmbargoExemptQuery(PlayaDatabase.CAMPS, "SELECT * FROM " + PlayaDatabase.CAMPS + " WHERE " + CampTable.name + " LIKE ? AND " + CampTable.latitude + " IS NOT NULL AND " + CampTable.latitude + " != 0", query)
                            .map(SqlBrite.Query::run)
                            .toBlocking()
                            .first();

                    r.values = newResult;
                    r.count = newResult.getCount();
                }
                return r;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                Timber.d("Publish %d result for %s", results.values == null ? 0 : ((Cursor) results.values).getCount(), constraint == null ? "None" : constraint.toString());

                if (results.values == null || results.count > 0) {
                    Timber.d("Publishing results to adapter");
                    changeCursor((Cursor) results.values);
                    notifyDataSetChanged();
                } else {
                    notifyDataSetInvalidated();
                }
            }

            @Override
            public CharSequence convertResultToString(Object result) {
                if (result instanceof Cursor) {
                    Cursor cursorResult = (Cursor) result;
                    return cursorResult.getString(cursorResult.getColumnIndex(PlayaItemTable.name));
                }
                return super.convertResultToString(result);
            }
        }
    }

    public interface HomeCampSelectionListener {
        void onHomeCampSelected(CampSelection selection);

        class CampSelection {

            public final String name;
            public final double lat;
            public final double lon;

            public CampSelection(double lat, double lon, String name) {
                this.lat = lat;
                this.lon = lon;
                this.name = name;
            }
        }
    }
}