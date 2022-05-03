package org.sensingkit.flaaslib;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.sensingkit.flaaslib.enums.Action;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;
import org.sensingkit.flaaslib.utils.Utils;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;
import org.sensingkit.flaaslib.workers.LocalTrainWorker;

import java.io.File;

public class FLaaSLibBroadcastReceiver extends AbstractBroadcastReceiver {

    @SuppressWarnings("unused")
    private static final String TAG = FLaaSLibBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Utils.getCurrentApp(context) == App.FLAAS) {
            Log.e(TAG, "Only RGB apps should receive this message");
            return;
        }

        Log.d(TAG, Utils.getApplicationName(context) + ": Received broadcast message.");

        Action action = Action.fromAction(intent.getAction());
        switch (action) {
            case SEND_STATUS:
                onStatusReceived(context, intent);
                break;
            case REQUEST_SAMPLES:
                onSamplesRequest(context, intent);
                break;
            case SEND_SAMPLES:
                onReceiveSamples(context, intent);
                break;
            case REQUEST_TRAINING:
                onTrainingRequest(context, intent);
                break;
            case SEND_WEIGHTS:
                // only applies to FLaaS
                break;
            default:
                Log.e(TAG, "Unrecognised action: " + action.getName());
        }
    }

    // An RGB app requested the samples
    private void onSamplesRequest(Context context, Intent intent) {

        // Check status
        if (!intent.getBooleanExtra(KEY_IS_SUCCESSFUL, false)) {
            Log.e(TAG, "onSamplesRequest was not successful");
            return;
        }

        // Get message details
        int fromAppID = intent.getIntExtra(KEY_FROM_APP_ID, -1);
        App toApp = App.fromId(fromAppID);
        int requestID = intent.getIntExtra(KEY_REQUEST_ID, -1);

        String dataset = intent.getStringExtra(KEY_DATASET);
        String datasetTypeString = intent.getStringExtra(KEY_DATASET_TYPE);
        DatasetType datasetType = DatasetType.fromName(datasetTypeString);

        int projectID = intent.getIntExtra(KEY_PROJECT_ID, -1);
        int round = intent.getIntExtra(KEY_ROUND, -1);

        // Now send the samples
        FLaaSLib.sendSamples(context, toApp, requestID, projectID, round, dataset, datasetType);
    }

    // Samples have been received
    private void onReceiveSamples(Context context, Intent intent) {

        // Check status
        if (!intent.getBooleanExtra(KEY_IS_SUCCESSFUL, false)) {
            Log.e(TAG, "onReceiveSamples was not successful");
            return;
        }

        // Get message details
        int fromAppID = intent.getIntExtra(KEY_FROM_APP_ID, -1);
        App fromApp = App.fromId(fromAppID);
        int requestID = intent.getIntExtra(KEY_REQUEST_ID, -1);

        String datasetTypeString = intent.getStringExtra(KEY_DATASET_TYPE);
        DatasetType datasetType = DatasetType.fromName(datasetTypeString);

        // parse message, add packets and return if data is complete
        boolean complete = addPacketsFromMessage(fromApp, requestID, intent);

        // check if complete
        if (complete) {

            // get complete data
            byte[] completeData = broadcastDataManager.getData(fromApp, requestID);

            // just save them and send ok
            File file = new File(context.getFilesDir(), datasetType.getFilename());
            Utils.saveData(file, completeData);
            FLaaSLib.sendSuccess(context, fromApp, requestID);

            broadcastDataManager.clear(fromApp, requestID);
        }
    }

    // FLaaS has requested training from RGB (so have sent model weights)
    private void onTrainingRequest(Context context, Intent intent) {

        // Check status
        if (!intent.getBooleanExtra(KEY_IS_SUCCESSFUL, false)) {
            Log.e(TAG, "onTrainingRequest was not successful");
            return;
        }

        // We can assume that RGB app received it
        if (Utils.getCurrentApp(context) == App.FLAAS) {
            Log.e(TAG, "Only RGB apps should be able to receive training requests");
            return;
        }

        // Get message details
        int fromAppID = intent.getIntExtra(KEY_FROM_APP_ID, -1);
        App fromApp = App.fromId(fromAppID);
        int requestID = intent.getIntExtra(KEY_REQUEST_ID, -1);

        // parse message, add packets and return if data is complete
        boolean complete = addPacketsFromMessage(fromApp, requestID, intent);

        // check if complete
        if (complete) {

            // Get more message details
            int projectId = intent.getIntExtra(KEY_PROJECT_ID, -1);
            int round = intent.getIntExtra(KEY_ROUND, -1);

            // get complete data
            byte[] completeData = broadcastDataManager.getData(fromApp, requestID);

            // just save them and send ok
            String prefix = projectId + "_" + round + "_";
            File file = new File(context.getFilesDir(), prefix + FLaaSLib.MODEL_WEIGHTS_FILENAME);
            Utils.saveData(file, completeData);
            FLaaSLib.sendSuccess(context, fromApp, requestID);

            // Get more message details
            String trainingMode = intent.getStringExtra(KEY_TRAINING_MODE);
            String model = intent.getStringExtra(KEY_MODEL);
            String username = intent.getStringExtra(KEY_USERNAME);
            String dataset = intent.getStringExtra(KEY_DATASET);
            String datasetTypeString = intent.getStringExtra(KEY_DATASET_TYPE);
            int epochs = intent.getIntExtra(KEY_EPOCHS, -1);
            int seed = intent.getIntExtra(KEY_SEED, -1);
            int maxSamples = intent.getIntExtra(KEY_MAX_SAMPLES, -1);

            // Schedule Training
            requestTrainAndSendWeights(context,
                    requestID, projectId, round, trainingMode, model, username, dataset, datasetTypeString, epochs, seed, maxSamples);
        }
    }

    private static void requestTrainAndSendWeights(Context context, int requestId, int projectId, int round, String trainingMode, String model, String username, String dataset, String datasetType, int epochs, int seed, int maxSamples) {

        // prepare data input
        Data inputData = new Data.Builder()
                .putInt(AbstractFLaaSWorker.KEY_REQUEST_ID_ARG, requestId)
                .putInt(AbstractFLaaSWorker.KEY_PROJECT_ID_ARG, projectId)
                .putInt(AbstractFLaaSWorker.KEY_ROUND_ARG, round)
                .putString(AbstractFLaaSWorker.KEY_TRAINING_MODE_ARG, trainingMode)
                .putString(AbstractFLaaSWorker.KEY_MODEL_ARG, model)
                .putString(AbstractFLaaSWorker.KEY_USERNAME_ARG, username)
                .putString(AbstractFLaaSWorker.KEY_DATASET_ARG, dataset)
                .putString(AbstractFLaaSWorker.KEY_DATASET_TYPE_ARG, datasetType)
                .putInt(AbstractFLaaSWorker.KEY_EPOCHS_ARG, epochs)
                .putInt(AbstractFLaaSWorker.KEY_SEED_ARG, seed)
                .putInt(AbstractFLaaSWorker.KEY_MAX_SAMPLES_ARG, maxSamples)
                .putLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, System.nanoTime())
                .build();

        // create workers
        OneTimeWorkRequest localTrainingWorker = new OneTimeWorkRequest.Builder(LocalTrainWorker.class)
                .setInputData(inputData)
                .addTag("FLaaS")
                .build();

        // add to the queue for execution
        WorkManager.getInstance(context).enqueue(localTrainingWorker);
    }
}
