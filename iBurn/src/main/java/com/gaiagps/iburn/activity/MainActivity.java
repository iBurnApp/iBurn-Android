package com.gaiagps.iburn.activity;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.DataUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.fragment.ArtListViewFragment;
import com.gaiagps.iburn.fragment.CampListViewFragment;
import com.gaiagps.iburn.fragment.EventListViewFragment;
import com.gaiagps.iburn.fragment.GoogleMapFragment;
import com.gaiagps.iburn.view.MapViewPager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static com.gaiagps.iburn.PlayaClient.isFirstLaunch;
import static com.gaiagps.iburn.PlayaClient.validateUnlockPassword;

public class MainActivity extends FragmentActivity implements SearchQueryProvider {
    public static final String TAG = "MainActivity";

    // Hold display width to allow MapViewPager to calculate
    // swiping margin on screen's right border.
    public static int display_width = -1;
    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private boolean googlePlayServicesMissing = false;

    private ViewPager mViewPager;
    private FragmentWithTitlePagerAdapter mPagerAdapter;
    private ImageView mSearchBtn;
    private EditText mSearchView;

    private String mCurFilter;

    /**
     * Fragments to appear in main ViewPager
     */
    private static List<Pair<Class<? extends Fragment>, Integer>> sPages
            = new ArrayList<Pair<Class<? extends Fragment>, Integer>>() {{
        add(new Pair<Class<? extends Fragment>, Integer>(GoogleMapFragment.class,        R.drawable.ic_map));
        add(new Pair<Class<? extends Fragment>, Integer>(ArtListViewFragment.class,      R.drawable.ic_brush));
        add(new Pair<Class<? extends Fragment>, Integer>(CampListViewFragment.class,     R.drawable.ic_tent));
        add(new Pair<Class<? extends Fragment>, Integer>(EventListViewFragment.class,    R.drawable.ic_clock));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getActionBar().hide();
        getDisplayWidth();
        setContentView(R.layout.activity_main);
        setupSearchButton();
        if (checkPlayServices()) {
            setupFragmentStatePagerAdapter();
        } else {
            googlePlayServicesMissing = true;
        }
        if (isFirstLaunch(this)) {
            showWelcomeDialog();
        }
        DataUtils.checkAndSetupDB(getApplicationContext());
        handleIntent(getIntent());
    }

    private boolean mSearching = false;

    private void setupSearchButton() {
        mSearchView = (EditText) findViewById(R.id.searchView);
        mSearchBtn = (ImageView) findViewById(R.id.searchBtn);

        mSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0 || s.length() > 1) {
                    dispatchSearchQuery(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        findViewById(R.id.searchBtn).setOnClickListener(new View.OnClickListener() {

            boolean isShowing = false;

            @Override
            public void onClick(View v) {
                if (!isShowing) {
                    expandSearchView();
                } else {
                    collapseSearchView();
                }
                isShowing = !isShowing;
            }
        });
    }

    private void expandSearchView() {
        if (mSearchView == null) return;
        mSearching = true;
        mSearchBtn.animate().alpha(1.0f).setDuration(300);
        mSearchView.animate().scaleX(1f).alpha(1.0f).translationX(0).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        mSearchView.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mSearchView, InputMethodManager.SHOW_FORCED);
        mSearchBtn.setImageResource(R.drawable.ic_x);
    }

    private void collapseSearchView() {
        if (mSearchView == null) return;
        mSearching = false;
        mSearchBtn.animate().alpha(0.7f).setDuration(300);
        mSearchView.animate().scaleX(.01f).alpha(0).translationX(330).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
        mSearchBtn.setImageResource(R.drawable.ic_search);
        mSearchView.setText("");
        mCurFilter = "";
        dispatchSearchQuery(mCurFilter);
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (googlePlayServicesMissing && checkPlayServices()) {
            setupFragmentStatePagerAdapter();
            googlePlayServicesMissing = false;
        }
    }

    private void showWelcomeDialog() {
        View dialog = getLayoutInflater().inflate(R.layout.dialog_welcome, null);
        new AlertDialog.Builder(this)
                .setView(dialog)
                .setPositiveButton(R.string.lets_burn, null)
                .show();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
            if (mPagerAdapter.getCurrentFragment() instanceof Searchable) {
                dispatchSearchQuery(query);
            } else
                Log.i(TAG, "Current fragment does not implement Searchable");
        }

        // TODO: Unused.. Consider removing
        if (intent.hasExtra("tab")) {
            Constants.TAB_TYPE tab = (Constants.TAB_TYPE) intent.getSerializableExtra("tab");
            switch (tab) {
                case MAP:
                    mViewPager.setCurrentItem(0, true);
                    LatLng marker = new LatLng(intent.getFloatExtra("lat", 0), intent.getFloatExtra("lon", 0));

                    ((GoogleMapFragment) mPagerAdapter.getItem(0)).mapAndCenterOnMarker(new MarkerOptions().position(marker).title(intent.getStringExtra("title")));
                    Log.i("GoogleMapFragment", "queue marker");
                    break;

            }
        }
    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        mCurFilter = query;
        if (mPagerAdapter.getCurrentFragment() instanceof Searchable) {
            Log.i(TAG, "dispatch query " + query);
            ((Searchable) mPagerAdapter.getCurrentFragment()).onSearchQueryRequested(query);
        }
    }

    private void setupFragmentStatePagerAdapter() {
        mViewPager = (MapViewPager) findViewById(R.id.pager);
        mPagerAdapter = new FragmentWithTitlePagerAdapter(getSupportFragmentManager(), sPages);
        mPagerAdapter.setSearchQueryProvider(this);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setBackgroundResource(R.drawable.pager_tab_bg);
        tabs.setShouldExpand(true);
        tabs.setTabPaddingLeftRight(0);
        tabs.setIndicatorColorResource(R.color.tab_selector);
        tabs.setTextColorResource(R.color.tab_text);
        tabs.setTextSize(10);
        tabs.setDividerColorResource(R.color.tab_divider);
        mViewPager.setAdapter(mPagerAdapter);
        tabs.setViewPager(mViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mSearching) {
            collapseSearchView();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (PlayaClient.isEmbargoClear(getApplicationContext()))
            menu.removeItem(R.id.action_unlock);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_unlock) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle("Enter Unlock Password");

            // Set an EditText view to get user input
            final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
            alert.setView(input);
            alert.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String pwGuess = input.getText().toString();
                    if (validateUnlockPassword(pwGuess)) {
                        PlayaClient.setEmbargoClear(MainActivity.this, true);
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.victory))
                                .setMessage(getString(R.string.location_data_unlocked))
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                                .show();
                    } else {
                        dialog.cancel();
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle(getString(R.string.invalid_password))
                                .show();
                    }
                }
            });

            alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                }
            });

            alert.show();
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String getCurrentQuery() {
        return mCurFilter;
    }

    /**
     * Adapter that takes a List of Pairs representing a Fragment and Title
     * for pairing with a tabbed ViewPager.
     * <p/>
     * Each Fragment must have a no-arg newInstance() method.
     */
    public static class FragmentWithTitlePagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        private static List<Pair<Class<? extends Fragment>, Integer>> PAGES;
        private Fragment mCurrentPrimaryItem;
        private SearchQueryProvider mSearchQueryProvider;
        private int mLastPosition;

        public FragmentWithTitlePagerAdapter(FragmentManager fm, List<Pair<Class<? extends Fragment>, Integer>> pages) {
            super(fm);
            PAGES = pages;
        }

        public void setSearchQueryProvider(SearchQueryProvider provider) {
            mSearchQueryProvider = provider;
        }

        @Override
        public int getCount() {
            return PAGES.size();
        }

        @Override
        public Fragment getItem(int position) {
            try {
                return (Fragment) PAGES.get(position).first.getMethod("newInstance", null).invoke(null, null);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                throw new IllegalStateException("Unexpected ViewPager item requested: " + position);
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentPrimaryItem = (Fragment) object;
            if (mLastPosition != position && mCurrentPrimaryItem instanceof Searchable &&
                    mSearchQueryProvider != null) {
                // Update the fragment with the current query
                ((Searchable) mCurrentPrimaryItem).onSearchQueryRequested(mSearchQueryProvider.getCurrentQuery());
                mLastPosition = position;
            }
        }

        public Fragment getCurrentFragment() {
            return mCurrentPrimaryItem;
        }

        @Override
        public int getPageIconResId(int i) {
            return PAGES.get(i).second;
        }
    }

    /**
     * Measure display width so the view pager can implement its
     * custom behavior re: paging on the map view
     */
    private void getDisplayWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        display_width = display.getWidth();
    }

    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, getString(R.string.device_not_supported),
                        Toast.LENGTH_LONG).show();
                finish();
            }

            return false;
        }
        return true;
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,
                REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, getString(R.string.requres_google_play),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
