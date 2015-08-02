package com.gaiagps.iburn.activity;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SECRETS;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.Subscriber;
import com.gaiagps.iburn.api.IBurnService;
import com.gaiagps.iburn.api.MockIBurnApi;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;
import com.gaiagps.iburn.fragment.BrowseListViewFragment;
import com.gaiagps.iburn.fragment.CampListViewFragment;
import com.gaiagps.iburn.fragment.EventListViewFragment;
import com.gaiagps.iburn.fragment.ExploreListViewFragment;
import com.gaiagps.iburn.fragment.GoogleMapFragment;
import com.gaiagps.iburn.fragment.PlayaListViewFragment;
import com.gaiagps.iburn.service.DataUpdateService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.InjectView;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements SearchQueryProvider {

    private static final String HOCKEY_ID = SECRETS.HOCKEY_ID;
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private boolean googlePlayServicesMissing = false;

    @InjectView(R.id.parent)
    ViewGroup mParent;

    @InjectView(R.id.pager)
    ViewPager mViewPager;

    @InjectView(R.id.tabs)
    TabLayout mTabs;

    private PrefsHelper prefs;
    private IBurnPagerAdapter mPagerAdapter;
    private String mCurFilter;

    /**
     * Fragments to appear in main ViewPager
     */
    private static List<IBurnPagerAdapter.IBurnTab> sTabs
            = new ArrayList<IBurnPagerAdapter.IBurnTab>() {{
        add(IBurnPagerAdapter.IBurnTab.MAP);
        add(IBurnPagerAdapter.IBurnTab.EXPLORE);
        add(IBurnPagerAdapter.IBurnTab.BROWSE);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (false) { //BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectCustomSlowCalls()
                    .penaltyLog()
                    .build());

//            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//                    .detect
//                    .penaltyLog()
//                    .build());
        }

        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

        mTabs.setTabGravity(TabLayout.GRAVITY_FILL);

        if (checkPlayServices()) {
            setupFragmentStatePagerAdapter();
        } else {
            googlePlayServicesMissing = true;
        }

        prefs = new PrefsHelper(this);

        if (!prefs.didShowWelcome()) {
            showWelcomeDialog();
        }

        if (!prefs.didScheduleUpdate()) {
            DataUpdateService.scheduleAutoUpdate(this);
            prefs.setDidScheduleUpdate(true);
        }

//        Mock update (Comment out DataService.scheduleAutoUpdate above)
//        IBurnService service = new IBurnService(this, new MockIBurnApi(this));
//        service.updateData().subscribe();

        if (Embargo.isEmbargoActive(prefs)) {
            Observable.timer(1, 1, TimeUnit.SECONDS)
                    .first()
                    .subscribeOn(AndroidSchedulers.mainThread())
                    .subscribe(counter -> {
                        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE M/d", Locale.US);
                        Snackbar.make(mParent, getString(R.string.embargo_snackbar_msg, dayFormatter.format(Embargo.EMBARGO_DATE)), Snackbar.LENGTH_INDEFINITE)
                                .setAction(R.string.enter_unlock_code, view -> showUnlockDialog()).show();
                    });
        }
        handleIntent(getIntent());
        checkForUpdates();
    }

    public void onSearchClick(View view) {
        startActivity(new Intent(this, SearchActivity.class));
    }


    @Override
    protected void onResume() {
        super.onResume();
        checkForCrashes();
        if (googlePlayServicesMissing && checkPlayServices()) {
            setupFragmentStatePagerAdapter();
            googlePlayServicesMissing = false;
        }
    }

    private void showWelcomeDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_welcome, null);
        final Dialog dialog = new AlertDialog.Builder(this, R.style.Theme_Iburn_Dialog)
                .setView(dialogView).create();
        dialogView.setOnClickListener(v -> {
            prefs.setDidShowWelcome(true);
        });
        dialog.show();
    }

    public void showUnlockDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.Theme_Iburn_Dialog);

        alert.setTitle(getString(R.string.enter_unlock_password));
        // Set an EditText view to get user input
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        alert.setView(input);
        alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
            String pwGuess = input.getText().toString();
            if (pwGuess.equals(SECRETS.UNLOCK_CODE)) {
                prefs.setEnteredValidUnlockCode(true);
                // Notify all observers that embargo is clear
                DataProvider.getInstance(this).subscribe(DataProvider::endUpgrade);
                new AlertDialog.Builder(MainActivity.this, R.style.Theme_Iburn_Dialog)
                        .setTitle(getString(R.string.victory))
                        .setMessage(getString(R.string.location_data_unlocked))
                        .setPositiveButton(R.string.ok, (dialog1, which) -> {
                        })
                        .show();
            } else {
                dialog.cancel();
                new AlertDialog.Builder(MainActivity.this, R.style.Theme_Iburn_Dialog)
                        .setTitle(getString(R.string.invalid_password))
                        .setMessage("Bummer.")
                        .show();
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), (dialog, whichButton) -> {
        });

        alert.show();
//        No effect :(
//        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//        imm.showSoftInput(input, InputMethodManager.SHOW_FORCED);
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
                Timber.d("Current fragment does not implement Searchable");
        }
    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        mCurFilter = query;
        if (mPagerAdapter.getCurrentFragment() instanceof Searchable) {
            Timber.d("dispatch query " + query);
            ((Searchable) mPagerAdapter.getCurrentFragment()).onSearchQueryRequested(query);
        }
    }

    private void setupFragmentStatePagerAdapter() {
        mPagerAdapter = new IBurnPagerAdapter(this, sTabs);
        mPagerAdapter.setSearchQueryProvider(this);

        mViewPager.setAdapter(mPagerAdapter);
        mTabs.setupWithViewPager(mViewPager);
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
     * for pairing with a tabbed ViewPager. Remove the IconTabProvider implementation
     * <p>
     * Each Fragment must have a no-arg newInstance() method.
     */
    public static class IBurnPagerAdapter extends FragmentPagerAdapter {

        private Context mContext;
        private List<IBurnTab> mTabs;
        private Fragment mCurrentPrimaryItem;
        private SearchQueryProvider mSearchQueryProvider;
        private int mLastPosition = -1;

        public enum IBurnTab {
            MAP(R.string.map_tab, R.drawable.ic_brc, GoogleMapFragment.class),
            EXPLORE(R.string.explore_tab, R.drawable.ic_calendar, ExploreListViewFragment.class),
            BROWSE(R.string.browse_tab, R.drawable.ic_camp, BrowseListViewFragment.class);

            private final Class<? extends Fragment> mFragClass;
            private final Integer mTitleResId;
            private final Integer mIconResId;

            private IBurnTab(final Integer titleResId,
                             final Integer iconResId,
                             final Class<? extends Fragment> fragClass) {
                mTitleResId = titleResId;
                mIconResId = iconResId;
                mFragClass = fragClass;
            }

            public Class<? extends Fragment> getFragmentClass() {
                return mFragClass;
            }

            public Integer getTitleResId() {
                return mTitleResId;
            }

            public Integer getIconResId() {
                return mIconResId;
            }
        }

        public IBurnPagerAdapter(FragmentActivity host, List<IBurnTab> tabs) {
            super(host.getSupportFragmentManager());
            mContext = host;
            mTabs = tabs;
        }

        public void setSearchQueryProvider(SearchQueryProvider provider) {
            mSearchQueryProvider = provider;
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public Fragment getItem(int position) {
            try {
                Fragment newFrag = mTabs.get(position).getFragmentClass().newInstance();
                return newFrag; //.getMethod("newInstance", null).invoke(null, null);
            } catch (Exception e) {
                // Actually (InstantiationException | IllegalAccessException), but we don't have
                // Java7 multi-catch pre API 19. We don't treat these exceptions separately,
                // so here we are catching Exception for now
                e.printStackTrace();
                throw new IllegalStateException("Unexpected ViewPager item requested: " + position);
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentPrimaryItem = (Fragment) object;
            if (mLastPosition != position) {

                if (mCurrentPrimaryItem instanceof Subscriber) {
                    Timber.d("Subscribing %d to data", position);
                    // We delay data subscription for a few milliseconds to allow
                    // the tab-switch transition to complete before layout occurs
                    Observable.timer(250, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                            .subscribe(time -> {
                                if (time == 0)
                                    ((Subscriber) mCurrentPrimaryItem).subscribeToData();
                            });
                }

                //if (mCurrentPrimaryItem instanceof Searchable && mSearchQueryProvider != null) {
                // Remove for now -- With a dedicated search screen we'll focus the list fragments
                // on browsing, not showing search results. If we decide to re-enable this
                // we should not deliver an unchanged search query for performance

                // Update the fragment with the current query
                //((Searchable) mCurrentPrimaryItem).onSearchQueryRequested(mSearchQueryProvider.getCurrentQuery());
                //}

                //String title = mContext.getString(sTabs.get(position).getTitleResId());
                //Log.i(TAG, "Setting tab title " + title);

                mLastPosition = position;
            }
        }

        public Fragment getCurrentFragment() {
            return mCurrentPrimaryItem;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mContext.getString(mTabs.get(position).getTitleResId());
        }

        public int getPageIconResId(int i) {
            return mTabs.get(i).getIconResId();
        }
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

    private void checkForCrashes() {
        CrashManager.register(this, HOCKEY_ID);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, HOCKEY_ID);
    }
}
