package org.sensingkit.flaaslib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.sensingkit.flaaslib.broadcastdata.BroadcastDataManager;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.ErrorCode;

public abstract class AbstractBroadcastReceiver extends BroadcastReceiver {

    @SuppressWarnings("unused")
    private static final String TAG = AbstractBroadcastReceiver.class.getSimpleName();

    // Constants (keys needed for communication)
    public static final String KEY_REQUEST_ID = "REQUEST_ID";
    public static final String KEY_IS_SUCCESSFUL = "IS_SUCCESSFUL";
    public static final String KEY_ERROR_ID = "ERROR_ID";
    public static final String KEY_FROM_APP_ID = "FROM_APP_ID";

    public static final String KEY_CURRENT_INDEX = "CURRENT_INDEX";
    public static final String KEY_TOTAL_PACKETS = "TOTAL_PACKETS";
    public static final String KEY_DATA_SIZE = "DATA_SIZE";
    public static final String KEY_DATA = "DATA";
    public static final String KEY_INCLUDES_METADATA = "INCLUDES_METADATA";
    public static final String KEY_METADATA = "METADATA";

    public static final String KEY_BACKEND_REQUEST_ID = "BACKEND_REQUEST_ID";
    public static final String KEY_PROJECT_ID = "PROJECT_ID";
    public static final String KEY_ROUND = "ROUND";
    public static final String KEY_TRAINING_MODE = "TRAINING_MODE";
    public static final String KEY_MODEL = "MODEL";
    public static final String KEY_DATASET = "DATASET";
    public static final String KEY_DATASET_TYPE = "DATASET_TYPE";
    public static final String KEY_EPOCHS = "EPOCHS";
    public static final String KEY_USERNAME = "USERNAME";
    public static final String KEY_SEED = "SEED";
    public static final String KEY_MAX_SAMPLES = "MAX_SAMPLES";
    public static final String KEY_STATS = "KEY_STATS";
    public static final String KEY_TIMESTAMP = "TIMESTAMP";
    public static final String KEY_VALID_DATE = "VALID_DATE";

    // Inherited properties
    protected static final BroadcastDataManager broadcastDataManager = BroadcastDataManager.getInstance();

    // An app sent a status (succeed or error)
    protected static void onStatusReceived(Context context, Intent intent) {

        int requestID = intent.getIntExtra(KEY_REQUEST_ID, -1);

        // Check status
        if (!intent.getBooleanExtra(KEY_IS_SUCCESSFUL, false)) {

            ErrorCode errorCode = ErrorCode.fromCode(intent.getIntExtra(KEY_ERROR_ID, -1));

            Log.e(TAG, "Request " + requestID + " was not successful. Error Code: " + errorCode);
            FLaaSLib.notifyStatusHandlers(requestID, true, errorCode);
        }
        else {
            Log.d(TAG, "OK");
            FLaaSLib.notifyStatusHandlers(requestID, true, ErrorCode.NO_ERROR);
        }
    }

    protected static boolean addPacketsFromMessage(App app, int requestID, Intent intent) {

        // Get data
        int currentIndex = intent.getIntExtra(KEY_CURRENT_INDEX, -1);
        int totalPackets = intent.getIntExtra(KEY_TOTAL_PACKETS, -1);
        int dataSize = intent.getIntExtra(KEY_DATA_SIZE, -1);
        byte[] data = intent.getByteArrayExtra(KEY_DATA);
        boolean includesMetadata = intent.getBooleanExtra(KEY_INCLUDES_METADATA, false);
        String metadata = intent.getStringExtra(KEY_METADATA);

        // First part, init data structure
        if (!broadcastDataManager.packetsExist(app, requestID)) {
            broadcastDataManager.initBroadcastData(app, requestID, dataSize, totalPackets, includesMetadata);
        }

        // complete data
        if (data != null) {
            int index = currentIndex * FLaaSLib.PACKET_MAX_SIZE;
            broadcastDataManager.addPackets(app, requestID, data, index);
        }
        if (metadata != null) {
            broadcastDataManager.addMetaData(app, requestID, metadata);
        }

        return broadcastDataManager.isComplete(app, requestID);
    }
}
