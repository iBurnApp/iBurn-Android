package com.gaiagps.iburn;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by davidbrodsky on 8/3/13.
 */
public class FileUtils {
    private static final String TAG = "FileUtils";

    private static void createDirectoryAndParents(Context c, String path) throws IOException {
        File toCreate = new File(path);
        if(!toCreate.exists())
            toCreate.mkdirs();
    }

    /**
     * Copies the MBTiles with name resourceId from res/raw to the external
     * path given
     * @param c
     * @param resourceId
     * @param tilesName the name of the MBTiles file to copy
     * @return true if copy occurred, false if destination already exists
     */
    public static boolean copyMBTilesToSD(Context c, int resourceId, String tilesName){
        String tilesPath = String.format("%s/%s/%s", Environment.getExternalStorageDirectory().getAbsolutePath(),
                                                    Constants.IBURN_ROOT, Constants.TILES_DIR);
        try {
            createDirectoryAndParents(c, tilesPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File output = new File(String.format("%s/%s", tilesPath, tilesName));
        if(!output.exists()){
            Log.i(TAG, "Copying MBTiles");
            copyRawFileToSD(c, resourceId, output.getAbsolutePath());
            Log.i(TAG, "MBTiles copying complete");
            return true;
        }else{
            Log.i(TAG, "MBTiles already copied");
            return false;
        }
    }


    private static void copyRawFileToSD(Context c, int resourceId, String outputPath){
        try {
            File tempFile = new File(outputPath);
            // TODO: Handle case where device has no SD card
            if(!tempFile.exists())
                tempFile.createNewFile();

            InputStream in = c.getResources().openRawResource(resourceId);
            FileOutputStream out = null;
            out = new FileOutputStream(outputPath);
            byte[] buff = new byte[1024];
            int read = 0;

            try {
                while ((read = in.read(buff)) > 0) {
                    out.write(buff, 0, read);
                }
            } finally {
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

    }
}
