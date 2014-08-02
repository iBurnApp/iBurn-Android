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
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.widget.*;

import com.astuetz.PagerSlidingTabStrip;
import com.gaiagps.iburn.DataUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.view.MapViewPager;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.fragment.ArtListViewFragment;
import com.gaiagps.iburn.fragment.CampListViewFragment;
import com.gaiagps.iburn.fragment.EventListViewFragment;
import com.gaiagps.iburn.fragment.GoogleMapFragment;
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
    private SearchView mSearchView;

    private String mCurFilter;

    /**
     * Fragments to appear in main ViewPager
     */
    private static List<Pair<Class<? extends Fragment>, String>> sPages
            = new ArrayList<Pair<Class<? extends Fragment>, String>>() {{
        add(new Pair<Class<? extends Fragment>, String>(GoogleMapFragment.class,        "Map"));
        add(new Pair<Class<? extends Fragment>, String>(ArtListViewFragment.class,      "Art"));
        add(new Pair<Class<? extends Fragment>, String>(CampListViewFragment.class,     "Camps"));
        add(new Pair<Class<? extends Fragment>, String>(EventListViewFragment.class,    "Events"));
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle("");
        getDisplayWidth();
        setContentView(R.layout.activity_main);
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
        if (TextUtils.isEmpty(query) && TextUtils.isEmpty(mCurFilter)) return;
        mCurFilter = query;
        if (mPagerAdapter.getCurrentFragment() instanceof Searchable) {
            ((Searchable) mPagerAdapter.getCurrentFragment()).onSearchQueryRequested(query);
        }
    }

    private void setupFragmentStatePagerAdapter() {
        mViewPager = (MapViewPager) findViewById(R.id.pager);
        mPagerAdapter = new FragmentWithTitlePagerAdapter(getSupportFragmentManager(), sPages);
        mPagerAdapter.setSearchQueryProvider(this);
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        tabs.setShouldExpand(true);
        tabs.setTabPaddingLeftRight(0);
        tabs.setIndicatorColor(getResources().getColor(R.color.highlight));
        tabs.setTextColorResource(R.color.white);
        mViewPager.setAdapter(mPagerAdapter);
        tabs.setViewPager(mViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        final MenuItem searchMenuItem = menu.findItem(R.id.search);
        mSearchView = (SearchView) searchMenuItem.getActionView();
//        mSearchView.setFocusable(false);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() == 0 || newText.length() > 2) {
                    dispatchSearchQuery(newText);
                }
                return false;
            }
        });
        mSearchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mSearchView != null && !mSearchView.isIconified()) {
            mSearchView.setIconified(true);
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
    public static class FragmentWithTitlePagerAdapter extends FragmentStatePagerAdapter {

        private static List<Pair<Class<? extends Fragment>, String>> PAGES;
        private Fragment mCurrentPrimaryItem;
        private SearchQueryProvider mSearchQueryProvider;
        private int mLastPosition;

        public FragmentWithTitlePagerAdapter(FragmentManager fm, List<Pair<Class<? extends Fragment>, String>> pages) {
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
        public CharSequence getPageTitle(int position) {
            return PAGES.get(position).second;
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
