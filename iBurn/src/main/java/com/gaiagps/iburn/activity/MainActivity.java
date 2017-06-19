package com.gaiagps.iburn.activity;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.gaiagps.iburn.MapboxMapFragment;
import com.gaiagps.iburn.PermissionManager;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;
import com.gaiagps.iburn.SearchQueryProvider;
import com.gaiagps.iburn.api.IBurnService;
import com.gaiagps.iburn.database.DataProvider;
import com.gaiagps.iburn.database.Embargo;
import com.gaiagps.iburn.fragment.BrowseListViewFragment;
import com.gaiagps.iburn.fragment.ExploreListViewFragment;
import com.gaiagps.iburn.fragment.FavoritesListViewFragment;
import com.gaiagps.iburn.fragment.MapPlaceHolderFragment;
import com.gaiagps.iburn.service.DataUpdateService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.roughike.bottombar.BottomBar;

import net.hockeyapp.android.CrashManager;
import net.hockeyapp.android.UpdateManager;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.RuntimePermissions;
import timber.log.Timber;

import static com.gaiagps.iburn.SECRETSKt.HOCKEY_ID;
import static com.gaiagps.iburn.SECRETSKt.UNLOCK_CODE;

@RuntimePermissions
public class MainActivity extends AppCompatActivity implements SearchQueryProvider {

    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private boolean googlePlayServicesMissing = false;
    private boolean awaitingLocationPermission = false;

    @BindView(R.id.parent)
    ViewGroup parent;

    @BindView(R.id.fab)
    FloatingActionButton fab;

    private PrefsHelper prefs;
    private String searchQuery;
    private BottomBar bottomBar;

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
        ButterKnife.bind(this);
        bottomBar = findViewById(R.id.bottomBar);
        setupBottomBar(bottomBar);

        prefs = new PrefsHelper(this);

        if (checkPlayServices()) {
            boolean haveLocationPermission = PermissionManager.hasLocationPermissions(getApplicationContext());

            if (prefs.didShowWelcome() && !haveLocationPermission) {

                setAwaitingLocationPermission(true);
                // Request location permission and notify onAcquiredLocationPermission on success
                MainActivityPermissionsDispatcher.onAcquiredLocationPermissionWithCheck(MainActivity.this);
            }
        }
        if (!checkPlayServices()) {
            googlePlayServicesMissing = true;
        }

        Timber.d("onCreate");
        if (!prefs.didShowWelcome()) {
            showWelcome();
            // TODO : No bundled DB yet, so pull data from API during onboarding
            Timber.d("Updating!");
            IBurnService service = new IBurnService(this);
            service.updateData()
                    .subscribe((success) -> {
                        Timber.d("Updated iburn with success %b", success);
                    }, throwable -> Timber.e(throwable, "Failed to update data"));
        }

        if (!prefs.didScheduleUpdate()) {
            DataUpdateService.scheduleAutoUpdate(this);
            prefs.setDidScheduleUpdate(true);
        }

//        Mock update (Comment out DataService.scheduleAutoUpdate above)
//        Timber.d("Updating!");
//        IBurnService service = new IBurnService(this);
//        service.updateData()
//                .subscribe((success) -> {
//                    Timber.d("Updated iburn with success %b", success);
//                });

        if (Embargo.isEmbargoActive(prefs)) {
            showEmbargoBanner();
        }
        handleIntent(getIntent());
        //checkForUpdates();
    }

    private void setAwaitingLocationPermission(boolean awaitingPermission) {
        this.awaitingLocationPermission = awaitingPermission;

        int currentTabId = bottomBar.getCurrentTabId();
        int mapTabId = R.id.tab_map;

        if (currentTabId == mapTabId) {
            setCurrentFragment(awaitingPermission ?
                    new MapPlaceHolderFragment() :
                    new MapboxMapFragment());
        }
    }

    private void setupBottomBar(BottomBar bottomBar) {
        bottomBar.setOnTabSelectListener(id -> {
            Fragment frag = null;

            switch (id) {
                case R.id.tab_map:
                    if (awaitingLocationPermission) {
                        frag = new MapPlaceHolderFragment();
                    } else {
                        frag = new MapboxMapFragment();
                    }
                    break;

                case R.id.tab_now:
                    frag = new ExploreListViewFragment();
                    break;

                case R.id.tab_browse:
                    frag = new BrowseListViewFragment();
                    break;

                case R.id.tab_favorites:
                    frag = new FavoritesListViewFragment();
                    break;
            }

            if (frag != null) {
                setCurrentFragment(frag);
            }
        });
    }

    private void setCurrentFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }

    public void onSearchClick(View view) {
        startActivity(new Intent(this, SearchActivity.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkForCrashes();
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

    private void checkForCrashes() {
        CrashManager.register(this, HOCKEY_ID);
    }

    private void checkForUpdates() {
        // Remove this for store builds!
        UpdateManager.register(this, HOCKEY_ID);
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
        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
        ViewGroup embargoBanner = (ViewGroup) inflater.inflate(R.layout.activity_main_embargo_banner, parent, false);
        RelativeLayout.LayoutParams embargoParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        embargoParams.addRule(RelativeLayout.ABOVE, R.id.bottomBar);
        embargoBanner.setLayoutParams(embargoParams);

        final SimpleDateFormat dayFormatter = new SimpleDateFormat("EEEE M/d", Locale.US);
        TextView embargoText = embargoBanner.findViewById(R.id.embargo_text);
        embargoText.setText(getString(R.string.embargo_msg, dayFormatter.format(Embargo.EMBARGO_DATE)));

        embargoBanner.setOnClickListener(view -> hideEmbargoBanner());
        embargoBanner.setAlpha(0);

        Button enterUnlockCodeBtn = embargoBanner.findViewById(R.id.enter_unlock_code_btn);
        enterUnlockCodeBtn.setOnClickListener(view -> {
            hideEmbargoBanner();
            showUnlockDialog();
        });

        parent.addView(embargoBanner);

        ValueAnimator alphaAnimator = ValueAnimator.ofFloat(0, 1);
        alphaAnimator.setDuration(1000);
        alphaAnimator.addUpdateListener(valueAnimator -> embargoBanner.setAlpha((Float) valueAnimator.getAnimatedValue()));

        // Fade in banner
        Flowable.timer(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(counter -> {
                    fab.hide();
                    alphaAnimator.start();
                });

        // Auto-dismiss banner
        Flowable.timer(11, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(counter -> hideEmbargoBanner());
    }

    private void hideEmbargoBanner() {
        final View embargoBanner = findViewById(R.id.embargo_banner);
        if (embargoBanner != null) {
            ValueAnimator fadeOutAnim = ValueAnimator.ofFloat(1, 0);
            fadeOutAnim.setDuration(1000);
            fadeOutAnim.addUpdateListener(valueAnimator -> {
                embargoBanner.setAlpha((Float) valueAnimator.getAnimatedValue());
                if (valueAnimator.getAnimatedFraction() == 0f) {
                    parent.removeView(embargoBanner);
                    fab.show();
                }
            });
            fadeOutAnim.start();
        }
    }
}
