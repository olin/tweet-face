package org.acmelab.tweetface;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Environment;

import java.io.File;

public class Util {
    private static final String TEMP_FILE_NAME = "tweet-face.jpg";
    private static final String TEMP_RESULT_FILE_NAME = "tweet-face-processed.jpg";
    private static final String TEMP_DIR = "/Android/data/org.acmelab.tweetface/tmp";

    public static boolean isOnline(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null) return false;
        return cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }

    public static void makeDirs() {
        File tempDir = new File(Environment.getExternalStorageDirectory(), TEMP_DIR);
        if (!tempDir.exists())
            tempDir.mkdirs();
    }

    public static File getTempFile() {
        return new File(Environment.getExternalStorageDirectory() + TEMP_DIR + "/" + TEMP_FILE_NAME);
    }

    public static File getResultTempFile() {
        return new File(Environment.getExternalStorageDirectory() + TEMP_DIR + "/" + TEMP_RESULT_FILE_NAME);
    }

    public static String getTempFilePath() {
        return Environment.getExternalStorageDirectory() + TEMP_DIR + "/" + TEMP_FILE_NAME;
    }

    public static String getResultTempFilePath() {
        return Environment.getExternalStorageDirectory() + TEMP_DIR + "/" + TEMP_RESULT_FILE_NAME;
    }

}
