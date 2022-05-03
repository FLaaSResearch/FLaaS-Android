package org.sensingkit.flaaslib.utils;

import android.content.Context;

import org.sensingkit.flaaslib.enums.App;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Utils {

    public static byte[] loadData(File file) throws IOException {
        return FileUtils.readFileToByteArray(file);
    }

    @SuppressWarnings("UnusedReturnValue")
    public static boolean saveData(File file, byte[] data) {

        // replace if exists
        if (file.exists()) //noinspection ResultOfMethodCallIgnored
            file.delete();

        // write file
        try {
            FileUtils.writeByteArrayToFile(file, data);
        }
        catch (IOException ex) {
            return false;
        }

        return true;
    }

    public static boolean writeInputStreamToDisk(InputStream inputStream, File file) {

        try {
            FileUtils.copyInputStreamToFile(inputStream, file);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getApplicationName(Context context) {
        return context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
    }

    public static App getCurrentApp(Context context) {
        String packageName = context.getPackageName();
        return App.fromPackageName(packageName);
    }
}
