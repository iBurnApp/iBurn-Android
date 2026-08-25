package com.gaiagps.iburn;

import android.content.Context;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class MapLibreNativeLibraryLoaderTest {

    @Test
    public void usesSystemLoaderWithoutFallbackWhenNativeLoadSucceeds() {
        Context context = mock(Context.class);
        AtomicBoolean fallbackCalled = new AtomicBoolean();
        TestReporter reporter = new TestReporter();
        MapLibreNativeLibraryLoader loader = new MapLibreNativeLibraryLoader(
                context,
                libraryName -> assertEquals("maplibre", libraryName),
                (ignoredContext, ignoredLibrary, ignoredVersion) -> fallbackCalled.set(true),
                reporter
        );

        loader.load("maplibre");

        assertFalse(fallbackCalled.get());
        assertTrue(reporter.normalLoadStarted);
        assertFalse(reporter.recovered);
        assertFalse(reporter.recoveryFailed);
    }

    @Test
    public void fallsBackToVersionedExtractionAfterSystemLoadFailure() {
        Context context = mock(Context.class);
        UnsatisfiedLinkError systemError = new UnsatisfiedLinkError("not found");
        AtomicReference<Context> fallbackContext = new AtomicReference<>();
        AtomicReference<String> fallbackLibrary = new AtomicReference<>();
        AtomicReference<String> fallbackVersion = new AtomicReference<>();
        TestReporter reporter = new TestReporter();
        MapLibreNativeLibraryLoader loader = new MapLibreNativeLibraryLoader(
                context,
                libraryName -> {
                    throw systemError;
                },
                (receivedContext, libraryName, version) -> {
                    fallbackContext.set(receivedContext);
                    fallbackLibrary.set(libraryName);
                    fallbackVersion.set(version);
                },
                reporter
        );

        loader.load("maplibre");

        assertSame(context, fallbackContext.get());
        assertEquals("maplibre", fallbackLibrary.get());
        assertEquals(String.valueOf(BuildConfig.VERSION_CODE), fallbackVersion.get());
        assertTrue(reporter.recovered);
        assertSame(systemError, reporter.systemLoadError);
        assertFalse(reporter.recoveryFailed);
    }

    @Test
    public void rethrowsFallbackFailureWithOriginalLoadErrorSuppressed() {
        Context context = mock(Context.class);
        UnsatisfiedLinkError systemError = new UnsatisfiedLinkError("not found");
        UnsatisfiedLinkError fallbackError = new UnsatisfiedLinkError("fallback failed");
        TestReporter reporter = new TestReporter();
        MapLibreNativeLibraryLoader loader = new MapLibreNativeLibraryLoader(
                context,
                libraryName -> {
                    throw systemError;
                },
                (ignoredContext, ignoredLibrary, ignoredVersion) -> {
                    throw fallbackError;
                },
                reporter
        );

        try {
            loader.load("maplibre");
            fail("Expected fallback failure");
        } catch (UnsatisfiedLinkError actual) {
            assertSame(fallbackError, actual);
        }

        assertEquals(1, fallbackError.getSuppressed().length);
        assertSame(systemError, fallbackError.getSuppressed()[0]);
        assertTrue(reporter.recoveryFailed);
        assertSame(fallbackError, reporter.recoveryError);
        assertFalse(reporter.recovered);
    }

    private static final class TestReporter
            implements MapLibreNativeLibraryLoader.RecoveryReporter {

        private boolean normalLoadStarted;
        private boolean recovered;
        private boolean recoveryFailed;
        private UnsatisfiedLinkError systemLoadError;
        private Throwable recoveryError;

        @Override
        public void normalLoadStarted() {
            normalLoadStarted = true;
        }

        @Override
        public void recovered(String libraryName, UnsatisfiedLinkError systemLoadError) {
            recovered = true;
            this.systemLoadError = systemLoadError;
        }

        @Override
        public void recoveryFailed(String libraryName, Throwable recoveryError) {
            recoveryFailed = true;
            this.recoveryError = recoveryError;
        }
    }
}
