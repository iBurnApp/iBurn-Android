package com.gaiagps.iburn.activity;

import static com.gaiagps.iburn.SECRETSKt.UNLOCK_CODE;

import android.Manifest;
import android.app.SearchManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;

import com.gaiagps.iburn.MapboxBundledMapKt;
import com.gaiagps.iburn.MapboxMapFragment;
import com.gaiagps.iburn.PermissionManager;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;
import com.gaiagps.iburn.databinding.ActivityMainBinding;
import com.gaiagps.iburn.fragment.BrowseListViewFragment;
import com.gaiagps.iburn.fragment.ExploreListViewFragment;
import com.gaiagps.iburn.fragment.FavoritesListViewFragment;
import com.gaiagps.iburn.fragment.MapPlaceHolderFragment;
import com.gaiagps.iburn.fragment.SearchFragment;
import com.gaiagps.iburn.service.DataUpdateService;
import com.gaiagps.iburn.view.BottomTickerView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements SearchQueryProvider {

    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private boolean googlePlayServicesMissing = false;
    private boolean awaitingLocationPermission = false;

    private ActivityMainBinding binding;

    private PrefsHelper prefs;
    private String searchQuery;
    private BottomNavigationView bottomBar;

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

        // Draw under status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
            int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            controller.setAppearanceLightStatusBars(nightMode != Configuration.UI_MODE_NIGHT_YES);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        bottomBar = findViewById(R.id.bottomBar);
        setupBottomBar(bottomBar);

        prefs = new PrefsHelper(this);

        if (checkPlayServices()) {
            boolean haveLocationPermission = PermissionManager.hasLocationPermissions(getApplicationContext());

            if (prefs.didShowWelcome() && !haveLocationPermission) {

                setAwaitingLocationPermission(true);
                // Request location permission and notify onAcquiredLocationPermission on success
                MainActivityPermissionsDispatcher.onAcquiredLocationPermissionWithPermissionCheck(MainActivity.this);
            }
        }
        if (!checkPlayServices()) {
            googlePlayServicesMissing = true;
        }

        Timber.d("onCreate");
        if (!prefs.didShowWelcome()) {
            showWelcome();
            MapboxBundledMapKt.copyBundledMap(getApplicationContext());
        }

        if (!prefs.didScheduleUpdate()) {
            DataUpdateService.scheduleAutoUpdate(this);
            prefs.setDidScheduleUpdate(true);
        }

        if (Embargo.isEmbargoActive(prefs)) {
            Flowable.timer(1, TimeUnit.SECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(tick -> {
                        showEmbargoBanner();
                    });
        }
        handleIntent(getIntent());

        // uncomment these to load updated JSON
//        Context context = getApplicationContext();
//        IBurnService service = new IBurnService(context, new MockIBurnApi(context));
//        service.updateData().subscribe(success -> Timber.d("Update result success: %b", success));
    }

    private void setAwaitingLocationPermission(boolean awaitingPermission) {
        this.awaitingLocationPermission = awaitingPermission;

        int currentTabId = bottomBar.getSelectedItemId();
        int mapTabId = R.id.tab_map;

        if (currentTabId == mapTabId) {
            setCurrentFragment(awaitingPermission ?
                    new MapPlaceHolderFragment() :
                    new MapboxMapFragment());
        }
    }

    private void setupBottomBar(BottomNavigationView bottomBar) {
        bottomBar.setOnItemSelectedListener(menuItem -> {
            Fragment frag = null;
            final int selectedId = menuItem.getItemId();
            if (R.id.tab_map == selectedId) {
                if (awaitingLocationPermission) {
                    frag = new MapPlaceHolderFragment();
                } else {
                    frag = new MapboxMapFragment();
                }
            } else if (R.id.tab_now == selectedId) {
                frag = new ExploreListViewFragment();
            } else if (R.id.tab_browse == selectedId) {
                frag = new BrowseListViewFragment();
            } else if (R.id.tab_favorites == selectedId) {
                frag = new FavoritesListViewFragment();
            } else if (R.id.tab_search == selectedId) {
                frag = new SearchFragment();
            }

            if (frag != null) {
                setCurrentFragment(frag);
            }
            return true;
        });
        bottomBar.setSelectedItemId(R.id.tab_map);
        bottomBar.setOnItemReselectedListener(menuItem -> { /* ignore re-selection */});
    }

    private void setCurrentFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commitAllowingStateLoss();
    }

    public void onSearchClick(View view) {
        startActivity(new Intent(this, SearchFragment.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (googlePlayServicesMissing && checkPlayServices()) {
            googlePlayServicesMissing = false;
        }
    }

    public void onStart() {
        super.onStart();
    }

    @OnPermissionDenied(Manifest.permission.ACCESS_FINE_LOCATION)
    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    void onAcquiredLocationPermission() {
        setAwaitingLocationPermission(false);
    }

    private void showWelcome() {
        Intent welcomeIntent = new Intent(getApplicationContext(), WelcomeActivity.class);
        welcomeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(welcomeIntent);
        finish();
    }

    public void showUnlockDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this, R.style.Theme_Iburn_Dialog);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View view = inflater.inflate(R.layout.dialog_unlock, findViewById(R.id.parent), false);
        final EditText input = view.findViewById(R.id.unlock_input);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);

        alert.setTitle(getString(R.string.enter_unlock_code));
        alert.setView(view);
        alert.setPositiveButton(getString(R.string.ok), (dialog, whichButton) -> {
            handleUnlockCodeGuess(input.getText().toString());
        });
        alert.setNegativeButton(getString(R.string.cancel), null);

        final AlertDialog dialog = alert.create();
        dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        input.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (handleUnlockCodeGuess(input.getText().toString())) {
                dialog.dismiss();
            }
            return true;
        });

        input.requestFocus();
        dialog.show();
    }

    private boolean handleUnlockCodeGuess(String guess) {
        if (guess.equals(UNLOCK_CODE)) {
            prefs.setEnteredValidUnlockCode(true);
            // Notify all observers that embargo is clear
            DataProvider.Companion.getInstance(getApplicationContext()).subscribe(DataProvider::endUpgrade);
            new AlertDialog.Builder(MainActivity.this, R.style.Theme_Iburn_Dialog)
                    .setTitle(getString(R.string.embargo_disabled))
                    .setMessage(getString(R.string.location_data_unlocked))
                    .setPositiveButton(R.string.ok, (dialog1, which) -> {
                    })
                    .show();
            return true;
        } else {
            new AlertDialog.Builder(MainActivity.this, R.style.Theme_Iburn_Dialog)
                    .setTitle(getString(R.string.invalid_password))
                    .setMessage("Bummer.")
                    .show();
            return false;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            //use the query to search your data somehow
//            if (mPagerAdapter.getCurrentFragment() instanceof Searchable) {
//                dispatchSearchQuery(query);
//            } else
//                Timber.d("Current fragment does not implement Searchable");
        }
    }

    /**
     * Dispatch a search query to the current Fragment in the FragmentPagerAdapter
     */
    private void dispatchSearchQuery(String query) {
        searchQuery = query;
//        if (mPagerAdapter.getCurrentFragment() instanceof Searchable) {
//            Timber.d("dispatch query '%s", query);
//            ((Searchable) mPagerAdapter.getCurrentFragment()).onSearchQueryRequested(query);
//        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public String getCurrentQuery() {
        return searchQuery;
    }


    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, getString(R.string.requres_play_services),
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
                    Toast.makeText(this, getString(R.string.requres_play_services),
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    /**
     * Show a banner above the bottom navigation bar describing the banner and allowing
     * the user to enter the embargo unlock code. Dismissed by touch anywhere but the
     * unlock button, or after a fixed interval set within.
     */
    private void showEmbargoBanner() {
        ViewGroup parent = findViewById(R.id.parent);

        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE M/d", Locale.US);

        String[] messages =
                new String[]{
                        getString(R.string.embargo_msg_1),
                        getString(R.string.embargo_msg_2, dayFormatter.format(Embargo.EMBARGO_DATE)).toUpperCase(),
                        getString(R.string.embargo_msg_3)

                };

        BottomTickerView ticker = new BottomTickerView(
                parent,
                getBottomBannerLayoutParams(),
                true,
                "DON'T PANIC",
                messages,
                24,
                4);
        ticker.setCallback(new BottomTickerView.Callback() {
            @Override
            public void onShown() {
                // no-op
            }

            @Override
            public void onDismissed() {
                // no-op
            }

            @Override
            public void onEnterUnlockCodeRequested() {
                showUnlockDialog();
            }
        });
        ticker.show();
    }

    /**
     * Like {@link #showEmbargoBanner()}, but not helpful
     */
    private void showBreakingNewsBanner() {
        ViewGroup parent = findViewById(R.id.parent);

        String[] messages = getResources().getStringArray(R.array.news_ticker);

        BottomTickerView ticker = new BottomTickerView(parent, getBottomBannerLayoutParams(), false, "BREAKING NEWS", messages, 12, 4);
        ticker.setCallback(new BottomTickerView.Callback() {
            @Override
            public void onShown() {
                // no-op
            }

            @Override
            public void onDismissed() {
                // no-op
            }

            @Override
            public void onEnterUnlockCodeRequested() {
                showUnlockDialog();
            }
        });
        ticker.show();
    }

    private ViewGroup.LayoutParams getBottomBannerLayoutParams() {
        RelativeLayout.LayoutParams bannerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        bannerParams.addRule(RelativeLayout.ABOVE, R.id.bottomBar);
        return bannerParams;
    }
}
