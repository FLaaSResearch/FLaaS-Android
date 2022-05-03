package org.sensingkit.flaas.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.sensingkit.flaaslib.FLaaSLib;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaas.MLModel;
import org.sensingkit.flaaslib.enums.JoinedRoundStatus;
import org.sensingkit.flaaslib.utils.PerformanceCheckpoint;
import org.sensingkit.flaaslib.utils.PersistentStore;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;

import java.io.File;
import java.io.IOException;

public class MergeModelsWorker extends AbstractNetworkFLaaSWorker {

    @SuppressWarnings("unused")
    private static final String TAG = MergeModelsWorker.class.getSimpleName();

    public static final int MODEL_SIZE = 627210;  // TODO: get value from model

    public MergeModelsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // performance measurement
        PerformanceCheckpoint totalPerformance = new PerformanceCheckpoint();

        // <-- Start
        totalPerformance.start();

        Log.d(TAG, "MergeModelsWorker is executing...");

        // prepare failure result
        Result failureResult = getFailureResult();

        // get context
        Context context = getApplicationContext();

        // get input data
        int requestId = getInputData().getInt(KEY_REQUEST_ID_ARG, -1);
        int backendRequestId = getInputData().getInt(KEY_BACKEND_REQUEST_ID_ARG, -1);
        int projectId = getInputData().getInt(KEY_PROJECT_ID_ARG, -1);
        int round = getInputData().getInt(KEY_ROUND_ARG, -1);
        String[] rgbStats = getInputData().getStringArray(KEY_APP_STATS_ARG);
        long receivedTime = getInputData().getLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, -1);
        long validDate = getInputData().getLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, -1);

        // If not valid, just return Failure (not retry)
        if (!isTaskValid(validDate)) {
            Log.d(TAG, "Task is not valid anymore and will be skipped.");
            joinRound(context, projectId, round, JoinedRoundStatus.MERGE_MODELS);
            return Result.failure();
        }

        // init stats
        String statsJson = getInputData().getString(KEY_STATS_ARG);
        loadStats(statsJson);

        // add worker started after X secs
        float workerStarted = (float)((System.nanoTime() - receivedTime) / 1e9);
        addProperty("merge_models_worker", "worker_started_after", workerStarted);

        // download_weights_worker here is not a bug
        float[] durationsStats = getInputData().getFloatArray(KEY_DURATIONS_ARG);
        if (durationsStats != null) {
            for (App app : App.rgbApps()) {
                addProperty("download_weights_worker", app.getName() + "_request_duration", durationsStats[app.getId()]);
            }
        }

        // Add the RGB stats to the stats object (local_training_worker is not a bug here)
        addAppStats("local_training_worker", rgbStats);

        // get list of models (should be 3 in total, one per app)
        File[] modelFiles = new File[App.rgbApps().length];
        for (App app: App.rgbApps()) {
            modelFiles[app.getId()] = PersistentStore.getWeightsFile(context, app, projectId, round);
        }

        // Write aggregated model
        String prefix = projectId + "_" + round + "_";
        File globalModelFile = new File(context.getFilesDir(), prefix + FLaaSLib.MODEL_WEIGHTS_FILENAME);
        try {
            MLModel.applyFedAvg(modelFiles, MODEL_SIZE, globalModelFile);
        } catch (IOException exception) {
            exception.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(exception);
            return failureResult;
        }

        // Clear weights
        PersistentStore.clearAllWeights(context, projectId, round);

        // End and report -->
        totalPerformance.end();
        addProperty("merge_models_worker", "worker_duration", totalPerformance.getDuration());

        // add attempt counter
        addProperty("merge_models_worker", "attempt", getRunAttemptCount());

        // Get stats file
        String jsonString = getStatsInJsonString();

        // Build output
        Data output = new Data.Builder()
                .putInt(KEY_REQUEST_ID_ARG, requestId)
                .putInt(KEY_BACKEND_REQUEST_ID_ARG, backendRequestId)
                .putInt(KEY_PROJECT_ID_ARG, projectId)
                .putInt(KEY_ROUND_ARG, round)
                .putLong(KEY_WORKER_SCHEDULED_TIME_ARG, System.nanoTime())
                .putString(KEY_STATS_ARG, jsonString)
                .putLong(KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        return Result.success(output);
    }

    private void addAppStats(@SuppressWarnings("SameParameterValue") String member, String[] rgbStats) {

        for (App app : App.rgbApps()) {
            String json = rgbStats[app.getId()];

            // with getAsJsonObject we go a level higher
            JsonObject appObject = new Gson().fromJson(json, JsonObject.class).getAsJsonObject(member);

            addJsonObject(member, app.getName(), appObject);
        }
    }

}
