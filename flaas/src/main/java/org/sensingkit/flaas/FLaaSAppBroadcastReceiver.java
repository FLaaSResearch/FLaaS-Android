package org.sensingkit.flaas;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.sensingkit.flaas.workers.MergeModelsWorker;
import org.sensingkit.flaas.workers.SubmitResultsWorker;
import org.sensingkit.flaaslib.AbstractBroadcastReceiver;
import org.sensingkit.flaaslib.FLaaSLib;
import org.sensingkit.flaaslib.enums.Action;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;
import org.sensingkit.flaaslib.utils.PersistentStore;
import org.sensingkit.flaaslib.utils.Utils;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;
import org.sensingkit.flaaslib.workers.LocalTrainWorker;

public class FLaaSAppBroadcastReceiver extends AbstractBroadcastReceiver {

    @SuppressWarnings("unused")
    private static final String TAG = FLaaSAppBroadcastReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (Utils.getCurrentApp(context) != App.FLAAS) {
            Log.e(TAG, "Only FLaaS app should receive this message");
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
                // only RGB apps should receive this message
                break;
            case SEND_WEIGHTS:
                onReceiveWeights(context, intent);
                break;
            default:
                Log.e(TAG, "Unrecognised action: " + action.getName());
        }
    }

    // FLaaS app has requested the samples
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

        int round = intent.getIntExtra(KEY_ROUND, -1);
        int projectID = intent.getIntExtra(KEY_PROJECT_ID, -1);
        String datasetTypeString = intent.getStringExtra(KEY_DATASET_TYPE);
        DatasetType datasetType = DatasetType.fromName(datasetTypeString);
        String trainingMode = intent.getStringExtra(KEY_TRAINING_MODE);
        String dataset = intent.getStringExtra(KEY_DATASET);

        // parse message, add packets and return if data is complete
        boolean complete = addPacketsFromMessage(fromApp, requestID, intent);

        // check if complete
        if (complete) {

            // get complete data
            byte[] completeData = broadcastDataManager.getData(fromApp, requestID);

            // get pending fields from PersistentStore
            int backendRequestID = PersistentStore.getIntDetails(context, projectID, round, KEY_BACKEND_REQUEST_ID);
            String model = PersistentStore.getStringDetails(context, projectID, round, KEY_MODEL);
            String username = PersistentStore.getStringDetails(context, projectID, round, KEY_USERNAME);
            int epochs = PersistentStore.getIntDetails(context, projectID, round, KEY_EPOCHS);
            int seed = PersistentStore.getIntDetails(context, projectID, round, KEY_SEED);
            int maxSamples = PersistentStore.getIntDetails(context, projectID, round, KEY_MAX_SAMPLES);
            float duration = (float)((System.nanoTime() - PersistentStore.getLongDetails(context, projectID, round, KEY_TIMESTAMP)) / 1e9);
            long validDate = PersistentStore.getLongDetails(context, projectID, round, KEY_VALID_DATE);

            FLaaSLib.sendSuccess(context, fromApp, requestID);

            // save data
            PersistentStore.samplesReceived(context, fromApp, projectID, round, datasetType, completeData, duration);

            if (PersistentStore.areSamplesComplete(context, projectID, round, datasetType)) {

                // get stats
                String jsonStats = PersistentStore.getStringDetails(context, projectID, round, KEY_STATS);

                // get all durations
                float[] durations = new float[App.rgbApps().length];
                for (App app: App.rgbApps()) {
                    durations[app.getId()] = PersistentStore.getSamplesDuration(context, app, projectID, round, datasetType);
                }

                // run scheduler to train locally with all RGB received samples
                requestTrainAndSubmit(context,
                        requestID, backendRequestID, projectID, round, trainingMode, model, username, dataset, datasetTypeString, epochs, seed, maxSamples, jsonStats, durations, validDate);

                // clear counters and training details
                PersistentStore.clearTrainingDetails(context, projectID, round);
            }

            broadcastDataManager.clear(fromApp, requestID);
        }
    }

    // Weights have been received from FLaaS (sent by RGB)
    private void onReceiveWeights(Context context, Intent intent) {

        // Check status
        if (!intent.getBooleanExtra(KEY_IS_SUCCESSFUL, false)) {
            Log.e(TAG, "onReceiveWeights was not successful");
            return;
        }

        // We can assume that FLaaS app received it
        if (Utils.getCurrentApp(context) != App.FLAAS) {
            Log.e(TAG, "Only FLaaS app should be able to receive weights");
            return;
        }

        // Get message details
        int fromAppID = intent.getIntExtra(KEY_FROM_APP_ID, -1);
        App fromApp = App.fromId(fromAppID);
        int requestID = intent.getIntExtra(KEY_REQUEST_ID, -1);
        int projectID = intent.getIntExtra(KEY_PROJECT_ID, -1);
        int round = intent.getIntExtra(KEY_ROUND, -1);

        // parse message, add packets and return if data is complete
        boolean complete = addPacketsFromMessage(fromApp, requestID, intent);

        // check if complete
        if (complete) {

            // get complete data
            byte[] completeData = broadcastDataManager.getData(fromApp, requestID);
            String metadata = broadcastDataManager.getMetadata(fromApp, requestID);
            float duration = (float)((System.nanoTime() - PersistentStore.getLongDetails(context, projectID, round, KEY_TIMESTAMP)) / 1e9);
            long validDate = PersistentStore.getLongDetails(context, projectID, round, KEY_VALID_DATE);

            FLaaSLib.sendSuccess(context, fromApp, requestID);

            // save data
            PersistentStore.weightsReceived(context, fromApp, projectID, round, completeData, metadata, duration);

            if (PersistentStore.areWeightsComplete(context, projectID, round)) {

                // retrieve saved properties from PersistentStore
                int backendRequestID = PersistentStore.getIntDetails(context, projectID, round, KEY_BACKEND_REQUEST_ID);

                // get stats
                String jsonStats = PersistentStore.getStringDetails(context, projectID, round, KEY_STATS);

                // create array of stats
                String[] rgbStats = new String[3];
                for (App app : App.rgbApps()) {
                    rgbStats[app.getId()] = PersistentStore.getWeightsMetadata(context, app, projectID, round);
                }

                // get all durations
                float[] durations = new float[App.rgbApps().length];
                for (App app: App.rgbApps()) {
                    durations[app.getId()] = PersistentStore.getWeightsDuration(context, app, projectID, round);
                }

                // run scheduler to merge and submit
                requestMergeAndSubmit(context, requestID, backendRequestID, projectID, round, jsonStats, rgbStats, durations, validDate);

                // clear them since not useful any more (details only, not weights and metadata)
                PersistentStore.clearTrainingDetails(context, projectID, round);
            }

            broadcastDataManager.clear(fromApp, requestID);
        }
    }

    // called once all samples are received (in Joint Samples mode)
    private static void requestTrainAndSubmit(Context context, int requestId, int backendRequestId, int projectId, int round, String trainingMode, String model, String username, String dataset, String datasetType, int epochs, int seed, int maxSamples, String jsonStats, float[] durations, long validDate) {

        // prepare data input
        Data inputData = new Data.Builder()
                .putInt(AbstractFLaaSWorker.KEY_REQUEST_ID_ARG, requestId)
                .putInt(AbstractFLaaSWorker.KEY_BACKEND_REQUEST_ID_ARG, backendRequestId)
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
                .putString(AbstractFLaaSWorker.KEY_STATS_ARG, jsonStats)
                .putFloatArray(AbstractFLaaSWorker.KEY_DURATIONS_ARG, durations)
                .putLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        // create workers
        OneTimeWorkRequest localTrainingWorker = new OneTimeWorkRequest.Builder(LocalTrainWorker.class)
                .setInputData(inputData)
                .addTag("FLaaS")
                .build();

        OneTimeWorkRequest submitResultsWorker = new OneTimeWorkRequest.Builder(SubmitResultsWorker.class)
                .addTag("FLaaS")
                .build();

        // add to the queue for execution
        WorkManager.getInstance(context)
                .beginWith(localTrainingWorker)
                .then(submitResultsWorker)
                .enqueue();
    }

    // Called once all models are received (in Joint Models mode)
    private static void requestMergeAndSubmit(Context context, int requestId, int backendRequestId, int projectId, int round, String jsonStats, String[] rgbStats, float[] durations, long validDate) {

        // prepare data input
        Data inputData = new Data.Builder()
                .putInt(AbstractFLaaSWorker.KEY_REQUEST_ID_ARG, requestId)
                .putInt(AbstractFLaaSWorker.KEY_BACKEND_REQUEST_ID_ARG, backendRequestId)
                .putInt(AbstractFLaaSWorker.KEY_PROJECT_ID_ARG, projectId)
                .putInt(AbstractFLaaSWorker.KEY_ROUND_ARG, round)
                .putString(AbstractFLaaSWorker.KEY_STATS_ARG, jsonStats)
                .putLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, System.nanoTime())
                .putStringArray(AbstractFLaaSWorker.KEY_APP_STATS_ARG, rgbStats)
                .putFloatArray(AbstractFLaaSWorker.KEY_DURATIONS_ARG, durations)
                .putLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        // create workers
        OneTimeWorkRequest mergeModelsWorker = new OneTimeWorkRequest.Builder(MergeModelsWorker.class)
                .setInputData(inputData)
                .addTag("FLaaS")
                .build();

        OneTimeWorkRequest submitResultsWorker = new OneTimeWorkRequest.Builder(SubmitResultsWorker.class)
                .addTag("FLaaS")
                .build();

        // add to the queue for execution
        WorkManager.getInstance(context)
                .beginWith(mergeModelsWorker)
                .then(submitResultsWorker)
                .enqueue();
    }
}
