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
import com.gaiagps.iburn.database.Camp;
import com.gaiagps.iburn.database.CampTable;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.PlayaItemTable;

import java.io.IOException;
import java.util.List;

import timber.log.Timber;

public class WelcomeFragment extends Fragment implements TextureView.SurfaceTextureListener {

    final static String LAYOUT_ID = "layoutid";

    // Welcome 1 - Show video
    private MediaPlayer mediaPlayer;
    private TextureView textureView;
    private Surface surface;

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

        if (surface != null) {
            surface.release();
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        surface = new Surface(surfaceTexture);

        try {
            AssetFileDescriptor descriptor = getActivity().getAssets().openFd("mp4/onboarding_loop_final.mp4");
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            mediaPlayer.setSurface(surface);
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

        private List<Camp> camps;
        private DataProvider dataProvider;
        private CampNameFilter filter;
        LayoutInflater inflater;

        public CampAutoCompleteAdapter(Context context) {
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            DataProvider.Companion.getInstance(context.getApplicationContext())
                    .subscribe(readyDataProvider -> this.dataProvider = readyDataProvider);
        }

        public void changeData(List<Camp> camps) {
            this.camps = camps;
        }

        @Override
        public int getCount() {
            return camps == null ? 0 : camps.size();
        }

        @Override
        public Object getItem(int position) {
            if (camps == null) return null;
            return camps.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (camps == null) return -1;
            return camps.get(position).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {
                convertView = new TextView(getActivity());
                convertView.setPadding(16, 16, 16, 16);
                ((TextView) convertView).setTextSize(16);
                ((TextView) convertView).setTextAppearance(getActivity(), R.style.PlayaTextItem);
            }

            if (camps != null) {
                Camp camp = camps.get(position);
                ((TextView) convertView).setText(camp.name);
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
                    String query = constraint.toString();// '%' + constraint.toString() + '%';
                    List<Camp> camps = dataProvider.observeCampsByName(query).blockingFirst();

                    r.values = camps;
                    r.count = camps.size();
                }
                return r;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                Timber.d("Publish %d result for %s", results.values == null ? 0 : ((List<Camp>) results.values).size(), constraint == null ? "None" : constraint.toString());

                if (results.values == null || results.count > 0) {
                    Timber.d("Publishing results to adapter");
                    changeData((List<Camp>) results.values);
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