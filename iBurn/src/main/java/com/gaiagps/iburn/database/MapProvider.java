package com.gaiagps.iburn.database;

import android.content.Context;
import android.os.Environment;

import com.gaiagps.iburn.Bytestreams;
import com.gaiagps.iburn.Constants;
import com.gaiagps.iburn.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

    public static final String MBTILE_DESTINATION = "iburn2015.mbtiles";

    private static MapProvider provider;

    public static MapProvider getInstance(Context context) {
        if (provider == null) provider = new MapProvider(context);
        return provider;
    }

    private Context context;
    private File mapDatabaseFile;
    private AtomicBoolean writingFile = new AtomicBoolean(false);

    // This subject alerts observers of changes to the database file
    private final PublishSubject<File> databaseSubject = PublishSubject.create();

    public MapProvider(Context context) {
        this.context = context;
    }

    public Observable<File> getMapDatabase() {
        if (mapDatabaseFile != null) return databaseSubject.startWith(mapDatabaseFile);

        mapDatabaseFile = getMBTilesFile(0); // TODO : Store shipping data version
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
        FileOutputStream fos = new FileOutputStream(dest);
        Bytestreams.copy(newMap, fos);
        newMap.close();
        fos.close();

        // Notify observers
        Timber.d("Notifying observers");
        databaseSubject.onNext(dest);
    }

    /**
     * @return the expected location of the MBTiles database. The file may not yet exist
     */
    private static File getMBTilesFile(long version) {
        return new File(String.format("%s/iburn/tiles/%s.%d", Environment.getExternalStorageDirectory().getAbsolutePath(),
                MBTILE_DESTINATION, version));
    }

    private static boolean copyResourceToFile(Context c, int resourceId, File destination) {
        try {
            File parent = destination.getParentFile();
            if (parent.mkdirs() || parent.isDirectory()) {
                Timber.d("Copying MBTiles");
                InputStream in = c.getResources().openRawResource(resourceId);
                Bytestreams.copy(in, new FileOutputStream(destination));
                Timber.d("MBTiles copying complete");
            }
        } catch (IOException e) {
            Timber.e(e, "Error copying MBTiles");
        }
        return false;
    }
}
