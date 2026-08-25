package com.gaiagps.iburn;

import android.content.Context;

import com.getkeepsafe.relinker.ReLinker;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.maplibre.android.LibraryLoader;

/**
 * Workaround for a library loading issue introduced in Android ~12 and reportedly fixed in 15.
 * Uses Android's native loader normally and extracts MapLibre from the installed APK only when
 * that loader fails. This keeps direct-from-APK loading for unaffected devices while working
 * around intermittent split-APK lookup failures.
 * See https://issuetracker.google.com/issues/346717090?pli=1
 */
final class MapLibreNativeLibraryLoader extends LibraryLoader {

    private static final String RECOVERY_VERSION = String.valueOf(BuildConfig.VERSION_CODE);

    private final Context context;
    private final SystemLoader systemLoader;
    private final FallbackLoader fallbackLoader;
    private final RecoveryReporter reporter;

    MapLibreNativeLibraryLoader(Context context) {
        this(
                applicationContext(context),
                System::loadLibrary,
                ReLinker::loadLibrary,
                new CrashlyticsRecoveryReporter()
        );
    }

    MapLibreNativeLibraryLoader(
            Context context,
            SystemLoader systemLoader,
            FallbackLoader fallbackLoader,
            RecoveryReporter reporter
    ) {
        this.context = context;
        this.systemLoader = systemLoader;
        this.fallbackLoader = fallbackLoader;
        this.reporter = reporter;
    }

    @Override
    public void load(String libraryName) {
        reporter.normalLoadStarted();
        try {
            systemLoader.load(libraryName);
            return;
        } catch (UnsatisfiedLinkError systemLoadError) {
            try {
                // Do not use ReLinker's recursive mode: MapLibre depends only on Android system
                // libraries, which the platform linker resolves after this library is extracted.
                fallbackLoader.load(context, libraryName, RECOVERY_VERSION);
                reporter.recovered(libraryName, systemLoadError);
            } catch (RuntimeException | UnsatisfiedLinkError recoveryError) {
                recoveryError.addSuppressed(systemLoadError);
                reporter.recoveryFailed(libraryName, recoveryError);
                throw recoveryError;
            }
        }
    }

    private static Context applicationContext(Context context) {
        Context applicationContext = context.getApplicationContext();
        return applicationContext != null ? applicationContext : context;
    }

    interface SystemLoader {
        void load(String libraryName);
    }

    interface FallbackLoader {
        void load(Context context, String libraryName, String version);
    }

    interface RecoveryReporter {
        void normalLoadStarted();

        void recovered(String libraryName, UnsatisfiedLinkError systemLoadError);

        void recoveryFailed(String libraryName, Throwable recoveryError);
    }

    private static final class CrashlyticsRecoveryReporter implements RecoveryReporter {

        private static final String USED_KEY = "maplibre_relinker_used";
        private static final String SUCCEEDED_KEY = "maplibre_relinker_succeeded";

        private final FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

        @Override
        public void normalLoadStarted() {
            // Crashlytics persists custom keys, so reset values for each process start.
            crashlytics.setCustomKey(USED_KEY, false);
            crashlytics.setCustomKey(SUCCEEDED_KEY, false);
        }

        @Override
        public void recovered(String libraryName, UnsatisfiedLinkError systemLoadError) {
            crashlytics.setCustomKey(USED_KEY, true);
            crashlytics.setCustomKey(SUCCEEDED_KEY, true);
            crashlytics.recordException(new MapLibreNativeLibraryRecoveredException(
                    libraryName,
                    systemLoadError
            ));
        }

        @Override
        public void recoveryFailed(String libraryName, Throwable recoveryError) {
            crashlytics.setCustomKey(USED_KEY, true);
            crashlytics.setCustomKey(SUCCEEDED_KEY, false);
            crashlytics.log("ReLinker failed to recover " + libraryName);
        }
    }

    private static final class MapLibreNativeLibraryRecoveredException
            extends RuntimeException {

        MapLibreNativeLibraryRecoveredException(
                String libraryName,
                UnsatisfiedLinkError systemLoadError
        ) {
            super("ReLinker recovered native library " + libraryName, systemLoadError);
        }
    }
}
