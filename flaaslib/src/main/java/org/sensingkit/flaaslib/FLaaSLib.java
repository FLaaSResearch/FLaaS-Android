package org.sensingkit.flaaslib;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import org.sensingkit.flaaslib.enums.Action;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;
import org.sensingkit.flaaslib.enums.ErrorCode;
import org.sensingkit.flaaslib.enums.TrainingMode;
import org.sensingkit.flaaslib.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FLaaSLib {

    @SuppressWarnings("unused")
    private static final String TAG = FLaaSLib.class.getSimpleName();

    private static final Random random = new Random();

    // Info about limits: https://www.neotechsoftware.com/blog/android-intent-size-limit
    public static final int PACKET_MAX_SIZE = 500000;

    private static final String PERMISSION = "org.sensingkit.flaas.permission.COMMUNICATE";

    public static final String MODEL_WEIGHTS_FILENAME = "model_weights.bin";

    private static final List<FLaaSStatusHandler> statusHandler = new ArrayList<>();

    private static int generateRequestID() {
        return random.nextInt() & Integer.MAX_VALUE;
    }

    // Private methods
    private static void sendData(Context context, Intent intent, byte[] data, String metaData) {

        // compute number of packets
        int packetsCount = data.length / PACKET_MAX_SIZE;
        if (data.length % PACKET_MAX_SIZE != 0) {
            packetsCount++;
        }

        boolean includesMetadata = metaData != null;

        // send packages
        for (int i = 0; i < packetsCount; i++) {
            int from = i * PACKET_MAX_SIZE;
            int to = Math.min(from + PACKET_MAX_SIZE, data.length);
            byte[] dataPacket = Arrays.copyOfRange(data, from, to);
            sendPacket(context, intent, dataPacket, i, packetsCount, data.length, includesMetadata);
        }

        // send the last bit (with metadata) if needed
        if (includesMetadata) {
            sendMetadataPacket(context, intent, packetsCount, data.length, metaData);
        }
    }

    private static void sendPacket(Context context, Intent intent, byte[] data, int currentIndex, int totalPackets, int dataSize, boolean includesMetaData) {

        // create packetIntent (copy of original intent)
        Intent packetIntent = new Intent(intent);

        // add data related extras
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_CURRENT_INDEX, currentIndex);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_TOTAL_PACKETS, totalPackets);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_DATA_SIZE, dataSize);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_DATA, data);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_INCLUDES_METADATA, includesMetaData);

        // Send
        context.sendBroadcast(packetIntent, PERMISSION);
    }

    private static void sendMetadataPacket(Context context, Intent intent, int totalPackets, int dataSize, String metaData) {

        // create packetIntent (copy of original intent)
        Intent packetIntent = new Intent(intent);

        // add data related extras
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_TOTAL_PACKETS, totalPackets);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_DATA_SIZE, dataSize);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_INCLUDES_METADATA, true);
        packetIntent.putExtra(AbstractBroadcastReceiver.KEY_METADATA, metaData);

        // Send
        context.sendBroadcast(packetIntent, PERMISSION);
    }

    // ---- Public API ----

    public static boolean isAppInstalled(Context context, App app) {

        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(app.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public static void registerStatusHandler(FLaaSStatusHandler handler) {
        statusHandler.add(handler);
    }

    public static void notifyStatusHandlers(int requestID, boolean isSuccessful, ErrorCode errorCode) {

        for (FLaaSStatusHandler callback : statusHandler) {
            callback.onStatusReceived(requestID, isSuccessful, errorCode);
        }
    }

    public static void sendSuccess(Context context, App toApp, int originalRequestID) {

        Log.d(TAG, "Sending success status...");

        Intent intent = new Intent();
        intent.setAction(Action.SEND_STATUS.getAction());
        intent.setPackage(toApp.getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        App app = Utils.getCurrentApp(context);
        intent.putExtra(AbstractBroadcastReceiver.KEY_REQUEST_ID, originalRequestID);
        intent.putExtra(AbstractBroadcastReceiver.KEY_IS_SUCCESSFUL, true);
        intent.putExtra(AbstractBroadcastReceiver.KEY_FROM_APP_ID, app.getId());

        context.sendBroadcast(intent, PERMISSION);
    }

    public static void sendError(Context context, App toApp, int originalRequestID, int errorCode) {

        Log.d(TAG, "Sending error status...");

        Intent intent = new Intent();
        intent.setAction(Action.SEND_STATUS.getAction());
        intent.setPackage(toApp.getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        App app = Utils.getCurrentApp(context);
        intent.putExtra(AbstractBroadcastReceiver.KEY_REQUEST_ID, originalRequestID);
        intent.putExtra(AbstractBroadcastReceiver.KEY_IS_SUCCESSFUL, false);
        intent.putExtra(AbstractBroadcastReceiver.KEY_ERROR_ID, errorCode);
        intent.putExtra(AbstractBroadcastReceiver.KEY_FROM_APP_ID, app.getId());
    }

    public static void requestSamples(Context context, App fromApp, String dataset, DatasetType datasetType, int projectID, int round) {

        Log.d(TAG, "Requesting samples...");

        Intent intent = new Intent();
        intent.setAction(Action.REQUEST_SAMPLES.getAction());
        intent.setPackage(fromApp.getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        App app = Utils.getCurrentApp(context);
        intent.putExtra(AbstractBroadcastReceiver.KEY_REQUEST_ID, generateRequestID());
        intent.putExtra(AbstractBroadcastReceiver.KEY_IS_SUCCESSFUL, true);
        intent.putExtra(AbstractBroadcastReceiver.KEY_FROM_APP_ID, app.getId());

        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET, dataset);
        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET_TYPE, datasetType.getName());

        intent.putExtra(AbstractBroadcastReceiver.KEY_PROJECT_ID, projectID);
        intent.putExtra(AbstractBroadcastReceiver.KEY_ROUND, round);

        context.sendBroadcast(intent, PERMISSION);
    }

    public static void sendSamples(Context context, App toApp, int requestID, int projectID, int round, String dataset, DatasetType datasetType) {

        Log.d(TAG, "Sending samples...");

        String filename;
        if (toApp == App.FLAAS) {  // Joint Samples mode
            filename = datasetType.getFilename();
        }
        else {  // RGB Setup mode
            filename = toApp.getName() + "_" + datasetType.getFilename();
        }

        File file = new File(context.getFilesDir(), filename);
        byte[] bytes;
        try {
            bytes = Utils.loadData(file);
        } catch (IOException e) {
            e.printStackTrace();
            FLaaSLib.sendError(context, toApp, requestID, 0);
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Action.SEND_SAMPLES.getAction());
        intent.setPackage(toApp.getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        App app = Utils.getCurrentApp(context);
        intent.putExtra(AbstractBroadcastReceiver.KEY_REQUEST_ID, requestID);
        intent.putExtra(AbstractBroadcastReceiver.KEY_IS_SUCCESSFUL, true);
        intent.putExtra(AbstractBroadcastReceiver.KEY_FROM_APP_ID, app.getId());

        intent.putExtra(AbstractBroadcastReceiver.KEY_PROJECT_ID, projectID);
        intent.putExtra(AbstractBroadcastReceiver.KEY_ROUND, round);
        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET, dataset);
        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET_TYPE, datasetType.getName());
        intent.putExtra(AbstractBroadcastReceiver.KEY_TRAINING_MODE, TrainingMode.JOINT_SAMPLES.getValue());

        // send data
        sendData(context, intent, bytes, null);
    }

    public static void requestTraining(Context context, App fromApp, String dataset, DatasetType datasetType, int projectId, int round, TrainingMode trainingMode, String model, String username, int epochs, int seed, int maxSamples) {

        Log.d(TAG, "Requesting training...");

        String prefix = projectId + "_" + round + "_";
        File file = new File(context.getFilesDir(), prefix + MODEL_WEIGHTS_FILENAME);
        byte[] bytes;
        try {
            bytes = Utils.loadData(file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Action.REQUEST_TRAINING.getAction());
        intent.setPackage(fromApp.getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        App app = Utils.getCurrentApp(context);
        intent.putExtra(AbstractBroadcastReceiver.KEY_REQUEST_ID, generateRequestID());
        intent.putExtra(AbstractBroadcastReceiver.KEY_IS_SUCCESSFUL, true);
        intent.putExtra(AbstractBroadcastReceiver.KEY_FROM_APP_ID, app.getId());

        intent.putExtra(AbstractBroadcastReceiver.KEY_PROJECT_ID, projectId);
        intent.putExtra(AbstractBroadcastReceiver.KEY_ROUND, round);
        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET_TYPE, datasetType.getName());
        intent.putExtra(AbstractBroadcastReceiver.KEY_TRAINING_MODE, trainingMode.getValue());
        intent.putExtra(AbstractBroadcastReceiver.KEY_MODEL, model);
        intent.putExtra(AbstractBroadcastReceiver.KEY_USERNAME, username);
        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET, dataset);
        intent.putExtra(AbstractBroadcastReceiver.KEY_DATASET_TYPE, datasetType.getName());
        intent.putExtra(AbstractBroadcastReceiver.KEY_EPOCHS, epochs);
        intent.putExtra(AbstractBroadcastReceiver.KEY_SEED, seed);
        intent.putExtra(AbstractBroadcastReceiver.KEY_MAX_SAMPLES, maxSamples);

        // send data
        sendData(context, intent, bytes, null);
    }

    public static void sendWeights(Context context, App toApp, int requestID, int projectId, int round, String statsJson) {

        Log.d(TAG, "Sending weights...");

        String prefix = projectId + "_" + round + "_";
        File file = new File(context.getFilesDir(), prefix + MODEL_WEIGHTS_FILENAME);
        byte[] bytes;
        try {
            bytes = Utils.loadData(file);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Intent intent = new Intent();
        intent.setAction(Action.SEND_WEIGHTS.getAction());
        intent.setPackage(toApp.getPackageName());
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);

        App app = Utils.getCurrentApp(context);
        intent.putExtra(AbstractBroadcastReceiver.KEY_REQUEST_ID, requestID);
        intent.putExtra(AbstractBroadcastReceiver.KEY_IS_SUCCESSFUL, true);
        intent.putExtra(AbstractBroadcastReceiver.KEY_FROM_APP_ID, app.getId());

        intent.putExtra(AbstractBroadcastReceiver.KEY_PROJECT_ID, projectId);
        intent.putExtra(AbstractBroadcastReceiver.KEY_ROUND, round);
        intent.putExtra(AbstractBroadcastReceiver.KEY_TRAINING_MODE, TrainingMode.JOINT_SAMPLES.getValue());

        // send data
        sendData(context, intent, bytes, statsJson);
    }
}
