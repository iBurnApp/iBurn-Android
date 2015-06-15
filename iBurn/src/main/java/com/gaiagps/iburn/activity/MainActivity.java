package com.gaiagps.iburn.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.astuetz.PagerSlidingTabStrip;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.DataUtils;
import com.gaiagps.iburn.PlayaClient;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SECRETS;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.Searchable;
import com.gaiagps.iburn.fragment.ArtListViewFragment;
import com.gaiagps.iburn.fragment.CampListViewFragment;
import com.gaiagps.iburn.fragment.EventListViewFragment;
import com.gaiagps.iburn.fragment.GoogleMapFragment;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

import static com.gaiagps.iburn.PlayaClient.isFirstLaunch;
import static com.gaiagps.iburn.PlayaClient.validateUnlockPassword;

public class MainActivity extends AppCompatActivity implements SearchQueryProvider {
    public static final String TAG = "MainActivity";

    private static final String HOCKEY_ID = SECRETS.HOCKEY_ID;
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private boolean googlePlayServicesMissing = false;

    @InjectView(R.id.toolbar)          Toolbar mToolbar;
    @InjectView(R.id.pager)            ViewPager mViewPager;
    @InjectView(R.id.search_button)    View mSearchButton;
    @InjectView(R.id.search_container) ViewGroup mSearchContainer;
    @InjectView(R.id.search)           EditText mSearchEntry;
    @InjectView(R.id.search_cancel)    View mSearchCancel;
    @InjectView(R.id.unlock_container) View mUnlockContainer;
    @InjectView(R.id.tabs)             TabLayout mTabs;

    private IBurnPagerAdapter mPagerAdapter;
    private String mCurFilter;

    /**
     * Fragments to appear in main ViewPager
     */
    private static List<IBurnPagerAdapter.IBurnTab> sTabs
            = new ArrayList<IBurnPagerAdapter.IBurnTab>() {{
        add(IBurnPagerAdapter.IBurnTab.MAP);
        add(IBurnPagerAdapter.IBurnTab.ART);
        add(IBurnPagerAdapter.IBurnTab.CAMPS);
        add(IBurnPagerAdapter.IBurnTab.EVENTS);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DataUtils.checkAndSetupDB(getApplicationContext());

        setContentView(R.layout.activity_main);
        ButterKnife.inject(this);

//        setSupportActionBar(mToolbar);

        setupSearchButton();
        if (checkPlayServices()) {
            setupFragmentStatePagerAdapter();
        } else {
            googlePlayServicesMissing = true;
        }
        if (isFirstLaunch(this)) {
            showWelcomeDialog();
        }
        if (PlayaClient.isEmbargoClear(this)) {
            mUnlockContainer.setVisibility(View.GONE);
        }
        handleIntent(getIntent());
        checkForUpdates();
    }

    private boolean mSearching = false;

    private void setupSearchButton() {

        mSearchEntry.addTextChangedListener(new TextWatcher() {
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

        mSearchEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(mSearchEntry.getWindowToken(), 0);
                return true;
            }
        });

        mSearchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                expandSearchView();
            }
        });

        mSearchCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collapseSearchView();
            }
        });
    }


    private void expandSearchView() {
        if (mSearchContainer == null) return;
//        mSearching = true;
//        mSearchBtn.animate().alpha(1.0f).setDuration(300);
//        mSearchView.animate().scaleX(1f).alpha(1.0f).translationX(0).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
//        mSearchView.requestFocus();
//        mSearchView.setEnabled(true);
//        mSearchBtn.setImageResource(R.drawable.ic_x);

        // get the center for the clipping circle
        int cx = mToolbar.getWidth();//(myView.getLeft() + myView.getRight()) / 2;
        int cy = (mSearchButton.getTop() + mSearchButton.getBottom()) / 2;

        // get the final radius for the clipping circle
        int finalRadius = mToolbar.getWidth(); //Math.max(myView.getWidth(), myView.getHeight());

        // create the animator for this view (the start radius is zero)
        Animator anim = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            anim = ViewAnimationUtils.createCircularReveal(mSearchContainer, cx, cy, 0, finalRadius);
            anim.start();
        }

        mSearchContainer.setVisibility(View.VISIBLE);

        // Focus and bring up keyboard
        mSearchEntry.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(mSearchEntry, InputMethodManager.SHOW_FORCED);
    }

    private void collapseSearchView() {
        if (mSearchContainer == null) return;
//        mSearching = false;
//        mSearchBtn.animate().alpha(0.7f).setDuration(300);
//        mSearchView.animate().scaleX(.01f).alpha(0).translationX(330).setDuration(300).setInterpolator(new AccelerateDecelerateInterpolator()).start();
//        mSearchView.setEnabled(false);
//        mSearchBtn.setImageResource(R.drawable.ic_search);
        mSearchEntry.setText("");
        mCurFilter = "";
        dispatchSearchQuery(mCurFilter);

        // get the center for the clipping circle
        int cx = mToolbar.getWidth();//(myView.getLeft() + myView.getRight()) / 2;
        int cy = (mSearchButton.getTop() + mSearchButton.getBottom()) / 2;

        // get the final radius for the clipping circle
        int initialRadius = mToolbar.getWidth(); //Math.max(myView.getWidth(), myView.getHeight());

        // create the animation (the final radius is zero)
        Animator anim = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            anim = ViewAnimationUtils.createCircularReveal(mSearchContainer, cx, cy, initialRadius, 0);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mSearchContainer.setVisibility(View.GONE);
                }
            });

            anim.start();
        } else {
            mSearchContainer.setVisibility(View.GONE);
        }

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mSearchEntry.getWindowToken(), 0);
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
        final Dialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();
        dialogView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void showUnlockDialog(final View v) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.enter_unlock_password));
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
                    mUnlockContainer.setVisibility(View.GONE);
                } else {
                    dialog.cancel();
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle(getString(R.string.invalid_password))
                            .setMessage("Bummer.")
                            .show();
                }
            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
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
                Log.i(TAG, "Current fragment does not implement Searchable");
        }

        // TODO: Unused.. Consider removing
        if (intent.hasExtra("tab")) {
            Constants.TabType tab = (Constants.TabType) intent.getSerializableExtra("tab");
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
        mPagerAdapter = new IBurnPagerAdapter(this, sTabs);
        mPagerAdapter.setSearchQueryProvider(this);

        mViewPager.setAdapter(mPagerAdapter);
        mTabs.setupWithViewPager(mViewPager);
    }

    @Override
    public void onBackPressed() {
        if (mSearching) {
            //collapseSearchView();
        } else {
            super.onBackPressed();
        }
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
     * <p/>
     * Each Fragment must have a no-arg newInstance() method.
     */
    public static class IBurnPagerAdapter extends FragmentPagerAdapter implements PagerSlidingTabStrip.IconTabProvider {

        private Context mContext;
        private List<IBurnTab> mTabs;
        private Fragment mCurrentPrimaryItem;
        private SearchQueryProvider mSearchQueryProvider;
        private int mLastPosition = -1;

        public static enum IBurnTab {
            MAP     (R.string.map_tab,    R.drawable.ic_brc,      GoogleMapFragment.class),
            ART     (R.string.art_tab,    R.drawable.ic_monument, ArtListViewFragment.class),
            CAMPS   (R.string.camps_tab,  R.drawable.ic_camp,     CampListViewFragment.class),
            EVENTS  (R.string.events_tab, R.drawable.ic_calendar, EventListViewFragment.class);

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
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
                throw new IllegalStateException("Unexpected ViewPager item requested: " + position);
            }
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mCurrentPrimaryItem = (Fragment) object;
            if (mLastPosition != position) {

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

        @Override
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
