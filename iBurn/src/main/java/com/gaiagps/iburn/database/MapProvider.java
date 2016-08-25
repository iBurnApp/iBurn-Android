package com.gaiagps.iburn.database;

import android.content.Context;
import android.os.Environment;

import com.gaiagps.iburn.Bytestreams;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.PrefsHelper;
import com.gaiagps.iburn.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Manages the MBTiles database, allowing clients to subscribe to a stream of events representing
 * changes to the database. This allows us to seamlessly update the underlying database while clients are
 * connected.
 * <p>
 * Created by davidbrodsky on 7/1/15.
 */
public class MapProvider {

    public static final String MBTILE_DESTINATION = "iburn2016.mbtiles";

    /** Key for mbtiles version stored in {@link PrefsHelper} and retrieved by {@link com.gaiagps.iburn.api.IBurnService}
     * for tile updating.
     */
    private static final String MBTILES_RESOURCE_NAME = "iburn.mbtiles.jar";

    private static MapProvider provider;

    public static MapProvider getInstance(Context context) {
        if (provider == null) provider = new MapProvider(context);
        return provider;
    }

    private Context context;
    private PrefsHelper prefs;
    private File mapDatabaseFile;
    private AtomicBoolean writingFile = new AtomicBoolean(false);

    // This subject alerts observers of changes to the database file
    private final PublishSubject<File> databaseSubject = PublishSubject.create();

    public MapProvider(Context context) {
        this.context = context;
        this.prefs = new PrefsHelper(context);
    }

    public Observable<File> getMapDatabase() {
        if (mapDatabaseFile != null) return databaseSubject.startWith(mapDatabaseFile);

        mapDatabaseFile = getMBTilesFile(prefs.getResourceVersion(MBTILES_RESOURCE_NAME));

        if (mapDatabaseFile.exists()) return databaseSubject.startWith(mapDatabaseFile);

        if (!writingFile.getAndSet(true)) {
            Observable.just(mapDatabaseFile)
                    .subscribeOn(Schedulers.io())
                    .doOnNext(destFile -> copyResourceToFile(context, R.raw.iburn, destFile))
                    .subscribe(databaseSubject::onNext);
        }

        return databaseSubject;
    }

    public void offerMapUpgrade(InputStream newMap, long version) throws IOException {
        File dest = getMBTilesFile(version);
        // Bug in version 12 created 'dest' as a directory, causing EISDIR error on attempted write
        if (dest.exists() && dest.isDirectory()) dest.delete();

        File destDirectory = dest.getParentFile();
        if (destDirectory.mkdirs() || destDirectory.isDirectory()) {
            FileOutputStream fos = new FileOutputStream(dest);
            Bytestreams.copy(newMap, fos);
            newMap.close();
            fos.close();

            // Notify observers
            Timber.d("Notifying observers of new database %s", dest.getAbsolutePath());
            databaseSubject.onNext(dest);
        } else {
            Timber.e("Tiles directory not available. Could not copy tiles");
        }
    }

    /**
     * @return the expected location of the MBTiles database. The file may not yet exist
     */
    private File getMBTilesFile(long version) {
        return new File(String.format("%s/iburn/tiles/%s.%d", context.getFilesDir().getAbsolutePath(),
                MBTILE_DESTINATION, version));
    }

    private static boolean copyResourceToFile(Context c, int resourceId, File destination) {
        try {
            File parent = destination.getParentFile();
            if (parent.mkdirs() || parent.isDirectory()) {
                Timber.d("Copying MBTiles");
                InputStream in = c.getResources().openRawResource(resourceId);
                Bytestreams.copy(in, new FileOutputStream(destination));
                Timber.d("MBTiles copying complete. Deleting old mbtiles");
                deleteMbTilesInDirectoryExcept(parent, destination);
            }
        } catch (IOException e) {
            Timber.e(e, "Error copying MBTiles");
        }
        return false;
    }

    private static void deleteMbTilesInDirectoryExcept(File directory, File doNotDelete) {
        FilenameFilter filter = (dir, filename) -> filename.contains(".mbtiles");
        File[] files = directory.listFiles(filter);
        for (File file : files) {
            if (!file.getAbsolutePath().equals(doNotDelete.getAbsolutePath())) {
                Timber.d("Will delete %s", file.getAbsolutePath());
                file.delete();
            }
        }
    }
}
