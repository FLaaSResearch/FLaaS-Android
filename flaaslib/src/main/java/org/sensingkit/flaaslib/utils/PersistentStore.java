package org.sensingkit.flaaslib.utils;

import android.content.Context;
import android.content.SharedPreferences;

import org.sensingkit.flaaslib.AbstractBroadcastReceiver;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;

import java.io.File;

public class PersistentStore {

    @SuppressWarnings("unused")
    private static final String TAG = PersistentStore.class.getSimpleName();

    private static final String PREFS_NAME = "PersistentStore";
    private static final String SAMPLES_PREFIX = "SAMPLES";
    private static final String WEIGHTS_PREFIX = "WEIGHTS";
    private static final String DETAILS_PREFIX = "DETAILS";

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Samples
    public static void samplesReceived(Context context, App fromApp, int projectId, int round, DatasetType datasetType, byte[] data, float duration) {

        SharedPreferences preferences = getPreferences(context);

        // Keys
        String counterKey = SAMPLES_PREFIX + "_" + datasetType.getName() + "_" + projectId + "_" + round + "_COUNTER";
        int counter = preferences.getInt(counterKey, 0);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(counterKey, counter + 1);

        String durationKey = SAMPLES_PREFIX + "_" + fromApp.getName() + "_" + datasetType.getName() + "_" + projectId + "_" + round + "_DURATION";
        editor.putFloat(durationKey, duration);
        editor.apply();

        // Files
        String filename = SAMPLES_PREFIX + "_" + fromApp.getName() + "_" + datasetType.getName() + "_" + projectId + "_" + round + ".bin";
        File file = new File(context.getFilesDir(), filename);
        Utils.saveData(file, data);
    }

    public static boolean areSamplesComplete(Context context, int projectId, int round, DatasetType datasetType) {
        SharedPreferences preferences = getPreferences(context);

        // Check counter
        String counterKey = SAMPLES_PREFIX + "_" + datasetType.getName() + "_" + projectId + "_" + round + "_COUNTER";
        int counter = preferences.getInt(counterKey, 0);

        return counter >= 3;  // Since 3 apps
    }

    public static File getSamplesFile(Context context, App ofApp, int projectId, int round, DatasetType datasetType) {
        String filename = SAMPLES_PREFIX + "_" + ofApp.getName() + "_" + datasetType.getName() + "_" + projectId + "_" + round + ".bin";
        return new File(context.getFilesDir(), filename);
    }

    public static float getSamplesDuration(Context context, App ofApp, int projectId, int round, DatasetType datasetType) {
        SharedPreferences preferences = getPreferences(context);

        String durationKey = SAMPLES_PREFIX + "_" + ofApp.getName() + "_" + datasetType.getName() + "_" + "_" + projectId + round + "_DURATION";
        return preferences.getFloat(durationKey, -1);
    }

    public static void clearAllSamples(Context context, int projectId, int round, DatasetType datasetType) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        // Reset counter
        String counterKey = SAMPLES_PREFIX + "_" + datasetType.getName() + "_" + projectId + "_" + round + "_COUNTER";
        editor.putInt(counterKey, 0);

        // clear metadata and durations
        for (App app: App.values()) {
            String durationKey = SAMPLES_PREFIX + "_" + app.getName() + "_" + datasetType.getName() + "_" + projectId + "_" + round + "_DURATION";
            editor.remove(durationKey);
        }

        editor.apply();

        // Delete Data
        for (App app: App.values()) {
            String filename = SAMPLES_PREFIX + "_" + app.getName() + "_" + datasetType.getName() + "_" + projectId + "_" + round + ".bin";
            File samplesFile = new File(context.getFilesDir(), filename);
            if (samplesFile.exists()) //noinspection ResultOfMethodCallIgnored
                samplesFile.delete();
        }
    }


    // Weights
    public static void weightsReceived(Context context, App fromApp, int projectId, int round, byte[] data, String metadata, float duration) {
        SharedPreferences preferences = getPreferences(context);

        // Keys
        String counterKey = WEIGHTS_PREFIX + "_" + projectId + "_" + round + "_COUNTER";
        String metadataKey = WEIGHTS_PREFIX + "_" + fromApp.getName() + "_" + projectId + "_" + round + "_METADATA";
        int counter = preferences.getInt(counterKey, 0);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(counterKey, counter + 1);
        editor.putString(metadataKey, metadata);

        String durationKey = WEIGHTS_PREFIX + "_" + fromApp.getName() + "_" + projectId + "_" + round + "_DURATION";
        editor.putFloat(durationKey, duration);
        editor.apply();

        // Files
        String filename = WEIGHTS_PREFIX + "_" + fromApp.getName() + "_" + projectId + "_" + round + ".bin";
        File file = new File(context.getFilesDir(), filename);
        Utils.saveData(file, data);
    }

    public static boolean areWeightsComplete(Context context, int projectId, int round) {
        SharedPreferences preferences = getPreferences(context);

        // Check counter
        String counterKey = WEIGHTS_PREFIX + "_" + projectId + "_" + round + "_COUNTER";
        int counter = preferences.getInt(counterKey, 0);

        return counter >= 3;  // Since 3 apps
    }

    public static File getWeightsFile(Context context, App ofApp, int projectId, int round) {
        String filename = WEIGHTS_PREFIX + "_" + ofApp.getName() + "_" + projectId + "_" + round + ".bin";
        return new File(context.getFilesDir(), filename);
    }

    public static String getWeightsMetadata(Context context, App ofApp, int projectId, int round) {
        SharedPreferences preferences = getPreferences(context);

        String metadataKey = WEIGHTS_PREFIX + "_" + ofApp.getName() + "_" + projectId + "_" + round + "_METADATA";
        return preferences.getString(metadataKey, null);
    }

    public static float getWeightsDuration(Context context, App ofApp, int projectId, int round) {
        SharedPreferences preferences = getPreferences(context);

        String durationKey = WEIGHTS_PREFIX + "_" + ofApp.getName() + "_" + projectId + "_" + round + "_DURATION";
        return preferences.getFloat(durationKey, -1);
    }

    public static void clearAllWeights(Context context, int projectId, int round) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        // Reset counter
        String counterKey = WEIGHTS_PREFIX + "_" + projectId + "_" + round + "_COUNTER";
        editor.putInt(counterKey, 0);

        // clear metadata and durations
        for (App app: App.values()) {
            String metadataKey = WEIGHTS_PREFIX + "_" + app.getName() + "_" + projectId + "_" + round + "_METADATA";
            String durationKey = WEIGHTS_PREFIX + "_" + app.getName() + "_" + projectId + "_" + round + "_DURATION";
            editor.remove(metadataKey);
            editor.remove(durationKey);
        }

        // apply
        editor.apply();

        // Delete Data
        for (App app: App.values()) {
            String filename = WEIGHTS_PREFIX + "_" + app.getName() + "_" + projectId + "_" + round + ".bin";
            File samplesFile = new File(context.getFilesDir(), filename);
            if (samplesFile.exists()) //noinspection ResultOfMethodCallIgnored
                samplesFile.delete();
        }
    }

    // Other Details
    public static void saveTrainingDetails(Context context, int projectId, int ofRound, int backendRequestID, String username, String model, int epochs, int seed, int maxSamples, String jsonStats, long validDate) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        // prepare key
        String key_prefix = DETAILS_PREFIX + "_" + projectId + "_" + ofRound + "_";

        editor.putInt(key_prefix + AbstractBroadcastReceiver.KEY_BACKEND_REQUEST_ID, backendRequestID);
        editor.putString(key_prefix + AbstractBroadcastReceiver.KEY_USERNAME, username);
        editor.putString(key_prefix + AbstractBroadcastReceiver.KEY_MODEL, model);
        editor.putInt(key_prefix + AbstractBroadcastReceiver.KEY_EPOCHS, epochs);
        editor.putInt(key_prefix + AbstractBroadcastReceiver.KEY_SEED, seed);
        editor.putInt(key_prefix + AbstractBroadcastReceiver.KEY_MAX_SAMPLES, maxSamples);
        editor.putString(key_prefix + AbstractBroadcastReceiver.KEY_STATS, jsonStats);
        editor.putLong(key_prefix + AbstractBroadcastReceiver.KEY_TIMESTAMP, System.nanoTime());
        editor.putLong(key_prefix + AbstractBroadcastReceiver.KEY_VALID_DATE, validDate);
        editor.apply();
    }

    public static boolean keyExistInDetails(Context context, int projectId, int ofRound, String key) {
        SharedPreferences preferences = getPreferences(context);

        // prepare key
        String completeKey = DETAILS_PREFIX + "_" + projectId + "_" + ofRound + "_" + key;
        return preferences.contains(completeKey);
    }

    public static String getStringDetails(Context context, int projectId, int ofRound, String key) {
        SharedPreferences preferences = getPreferences(context);

        // prepare key
        String completeKey = DETAILS_PREFIX + "_" + projectId + "_" + ofRound + "_" + key;
        return preferences.getString(completeKey, null);
    }

    public static int getIntDetails(Context context, int projectId, int ofRound, String key) {
        SharedPreferences preferences = getPreferences(context);

        // prepare key
        String completeKey = DETAILS_PREFIX + "_" + projectId + "_" + ofRound + "_" + key;
        return preferences.getInt(completeKey, -1);
    }

    public static long getLongDetails(Context context, int projectId, int ofRound, String key) {
        SharedPreferences preferences = getPreferences(context);

        // prepare key
        String completeKey = DETAILS_PREFIX + "_" + projectId + "_" + ofRound + "_" + key;
        return preferences.getLong(completeKey, -1);
    }

    public static void clearTrainingDetails(Context context, int projectId, int ofRound) {
        SharedPreferences preferences = getPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        // prepare key
        String keyPrefix = DETAILS_PREFIX + "_" + projectId + "_" + ofRound + "_";

        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_BACKEND_REQUEST_ID);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_USERNAME);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_MODEL);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_EPOCHS);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_SEED);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_MAX_SAMPLES);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_STATS);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_TIMESTAMP);
        editor.remove(keyPrefix + AbstractBroadcastReceiver.KEY_VALID_DATE);

        editor.apply();
    }
}
