package com.gaiagps.iburn.fragment;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.gaiagps.iburn.DateUtil;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.database.Camp;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;

public class WelcomeFragment extends Fragment implements TextureView.SurfaceTextureListener {

    final static String LAYOUT_ID = "layoutid";

    // Welcome 1 - Show video
    private MediaPlayer mediaPlayer;
    private TextureView textureView;
    private Surface surface;

    // Welcome 3 - Set Home
    private AutoCompleteTextView campSearchView;

    private boolean performedEntranceAnimation;

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
            textureView = rootView.findViewById(R.id.video);
            textureView.setSurfaceTextureListener(this);

        } else if (getArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment2) { {

            final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE MMMM d", Locale.US);
            dayFormatter.setTimeZone(DateUtil.PLAYA_TIME_ZONE);
            String embargoDate = dayFormatter.format(Embargo.EMBARGO_DATE);
            ((TextView) rootView.findViewById(R.id.content)).setText(getString(R.string.location_data_notice, embargoDate));

        }} if (getArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment3) {
            // Set Home location
            ImageView brcView = rootView.findViewById(R.id.parallax0);
            Drawable brcMap = brcView.getDrawable().mutate();
            brcMap.setTint(rootView.getContext().getColor(R.color.regular_text));
            brcView.setImageDrawable(brcMap);
            campSearchView = rootView.findViewById(R.id.campNameSearch);
            campSearchView.setAdapter(new CampAutoCompleteAdapter(getActivity()));
            campSearchView.setOnItemClickListener((parent, view, position, id) -> {
                Camp selectedCamp = ((Camp) campSearchView.getAdapter().getItem(position));

                if (!selectedCamp.hasLocation() && !selectedCamp.hasUnofficialLocation()) {
                    rootView.findViewById(R.id.error).setVisibility(View.VISIBLE);
                    campSearchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    if (getActivity() instanceof HomeCampSelectionListener) {
                        ((HomeCampSelectionListener) getActivity()).onHomeCampSelected(null);
                    }
                    return;
                } else {
                    rootView.findViewById(R.id.error).setVisibility(View.GONE);
                    campSearchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_green_24dp, 0);
                }

                campSearchView.setTag(selectedCamp);
                Timber.d("Item selected %s", campSearchView.getText().toString());

                InputMethodManager inputManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(campSearchView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                if (getActivity() instanceof HomeCampSelectionListener) {
                    ((HomeCampSelectionListener) getActivity()).onHomeCampSelected(selectedCamp);
                }
            });
        }
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean isWelcome1 = getArguments().getInt(LAYOUT_ID, -1) == R.layout.welcome_fragment1;
        if (isWelcome1 && !performedEntranceAnimation) {
            final View heading = getView().findViewById(R.id.heading);
            heading.setAlpha(0);
            ValueAnimator fadeIn = ValueAnimator.ofFloat(0, 1);
            fadeIn.addUpdateListener(animation -> heading.setAlpha((Float) animation.getAnimatedValue()));
            fadeIn.setStartDelay(1000);
            fadeIn.setDuration(1 * 1000);
            fadeIn.start();

            final View subHeading = getView().findViewById(R.id.sub_heading);
            subHeading.setAlpha(0);
            heading.setAlpha(0);
            ValueAnimator subFadeIn = ValueAnimator.ofFloat(0, 1);
            subFadeIn.addUpdateListener(animation -> subHeading.setAlpha((Float) animation.getAnimatedValue()));
            subFadeIn.setStartDelay(2000);
            subFadeIn.setDuration(1 * 1000);
            subFadeIn.start();
            performedEntranceAnimation = true;
        }
    }

    @Override
    public void onDestroyView() {
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
        public Camp getItem(int position) {
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
                if (result instanceof Camp) {
                    return ((Camp) result).name;
                }
                return super.convertResultToString(result);
            }
        }
    }

    public interface HomeCampSelectionListener {
        void onHomeCampSelected(Camp homeCamp);
    }
}