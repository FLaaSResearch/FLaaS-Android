package org.sensingkit.flaas.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.sensingkit.flaas.model.RetroRound;
import org.sensingkit.flaas.network.APIService;
import org.sensingkit.flaas.rest.NetworkManager;
import org.sensingkit.flaaslib.FLaaSLib;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;
import org.sensingkit.flaaslib.enums.JoinedRoundStatus;
import org.sensingkit.flaaslib.enums.TrainingMode;
import org.sensingkit.flaaslib.utils.PerformanceCheckpoint;
import org.sensingkit.flaaslib.utils.PersistentStore;
import org.sensingkit.flaaslib.utils.Utils;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;

import java.io.File;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadWeightsWorker extends AbstractNetworkFLaaSWorker {

    @SuppressWarnings("unused")
    private static final String TAG = DownloadWeightsWorker.class.getSimpleName();

    public DownloadWeightsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // performance measurement
        PerformanceCheckpoint totalPerformance = new PerformanceCheckpoint();

        // <-- Start
        totalPerformance.start();

        // prepare failure result
        Result failureResult = getFailureResult();

        // get context
        Context context = getApplicationContext();

        // get input data
        int backendRequestID = getInputData().getInt(KEY_BACKEND_REQUEST_ID_ARG, -1);
        int projectId = getInputData().getInt(KEY_PROJECT_ID_ARG, -1);
        int round = getInputData().getInt(KEY_ROUND_ARG, -1);
        String trainingModeString = getInputData().getString(KEY_TRAINING_MODE_ARG);
        TrainingMode trainingMode = TrainingMode.fromValue(trainingModeString);
        String username = getInputData().getString(KEY_USERNAME_ARG);
        long validDate = getInputData().getLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, -1);
        long receivedTime = getInputData().getLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, -1);
        long receivedLocalTime = getInputData().getLong(AbstractFLaaSWorker.KEY_LOCAL_TIME_ARG, -1);

        // If not valid, just return Failure (not retry)
        if (!isTaskValid(validDate)) {
            Log.d(TAG, "Task is not valid anymore and will be skipped.");
            joinRound(context, projectId, round, JoinedRoundStatus.DOWNLOAD_MODEL);
            return Result.failure();
        }

        // init stats (with null stats since its the first worker in the pipeline)
        String statsJson = getInputData().getString(KEY_STATS_ARG);
        loadStats(statsJson);

        // add generic stats / info
        addProperty("general", "notification_received_time", receivedTime);
        addProperty("general", "backend_request_id", backendRequestID);
        addProperty("general", "notification_received_local_time", receivedLocalTime);

        // add worker started after X secs
        float workerStarted = (float)((System.nanoTime() - receivedTime) / 1e9);
        addProperty("download_weights_worker", "worker_started_after", workerStarted);

        // Join round
        Log.d(TAG, "Joining round...");
        PerformanceCheckpoint joinRoundPerformance = new PerformanceCheckpoint();

        // <-- Start
        joinRoundPerformance.start();

        RetroRound roundDetails = joinRound(context, projectId, round, JoinedRoundStatus.JOIN_ROUND);
        if (roundDetails == null) {
            Log.e(TAG, "Join round failed.");
            return failureResult;
        }

        // End and report -->
        joinRoundPerformance.end();
        addProperty("download_weights_worker", "join_round_duration", joinRoundPerformance.getDuration());

        // download and save model weights
        Log.d(TAG, "Downloading weights...");
        if (!downloadWeights(context, projectId, round)) {
            Log.e(TAG, "Download weights failed.");
            return failureResult;
        }

        if (trainingMode == TrainingMode.JOINT_MODELS) {

            DatasetType datasetType = DatasetType.fromName(roundDetails.getDatasetType());

            // Save details in the PersistentStore for getting them later when all samples are received
            String model = roundDetails.getModel();
            int epochs = roundDetails.getNumberOfEpochs();
            int seed = roundDetails.getSeed();
            int maxSamples = roundDetails.getNumberOfSamples();
            String dataset = roundDetails.getDataset();

            // End and report -->
            totalPerformance.end();
            addProperty("download_weights_worker", "worker_duration", totalPerformance.getDuration());

            // add attempt counter
            addProperty("download_weights_worker", "attempt", getRunAttemptCount());

            // Get stats json string
            String jsonString = getStatsInJsonString();

            PersistentStore.saveTrainingDetails(context, projectId, round, backendRequestID, username, model, epochs, seed, maxSamples, jsonString, validDate);

            // Send request for training
            for (App app : App.rgbApps()) {
                FLaaSLib.requestTraining(context, app, dataset, datasetType, projectId, round, trainingMode, model, username, epochs, seed, maxSamples);
            }

            return Result.success();
        }
        else if (trainingMode == TrainingMode.JOINT_SAMPLES) {

            String dataset = roundDetails.getDataset();
            DatasetType datasetType = DatasetType.fromName(roundDetails.getDatasetType());

            // Save details in the PersistentStore for getting them later when all samples are received
            String model = roundDetails.getModel();
            int epochs = roundDetails.getNumberOfEpochs();
            int seed = roundDetails.getSeed();
            int maxSamples = roundDetails.getNumberOfSamples();

            // End and report -->
            totalPerformance.end();
            addProperty("download_weights_worker", "worker_duration", totalPerformance.getDuration());

            // add attempt counter
            addProperty("download_weights_worker", "attempt", getRunAttemptCount());

            // Get stats json string
            String jsonString = getStatsInJsonString();

            PersistentStore.saveTrainingDetails(context, projectId, round, backendRequestID, username, model, epochs, seed, maxSamples, jsonString, validDate);

            // Send request for samples
            for (App app : App.rgbApps()) {
                FLaaSLib.requestSamples(context, app, dataset, datasetType, projectId, round);
            }

            return Result.success();
        }

        // End and report -->
        totalPerformance.end();
        addProperty("download_weights_worker", "worker_duration", totalPerformance.getDuration());

        // add attempt counter
        addProperty("download_weights_worker", "attempt", getRunAttemptCount());

        // Get stats json string
        String jsonString = getStatsInJsonString();

        // Build output
        Data output = new Data.Builder()
                .putInt(KEY_BACKEND_REQUEST_ID_ARG, backendRequestID)
                .putInt(KEY_PROJECT_ID_ARG, projectId)
                .putInt(KEY_ROUND_ARG, round)
                .putString(KEY_TRAINING_MODE_ARG, trainingModeString)
                .putString(KEY_USERNAME_ARG, username)
                .putString(KEY_DATASET_ARG, roundDetails.getDataset())
                .putString(KEY_DATASET_TYPE_ARG, roundDetails.getDatasetType())
                .putInt(KEY_EPOCHS_ARG, roundDetails.getNumberOfEpochs())
                .putInt(KEY_SEED_ARG, roundDetails.getSeed())
                .putInt(KEY_MAX_SAMPLES_ARG, roundDetails.getNumberOfSamples())
                .putString(KEY_MODEL_ARG, roundDetails.getModel())
                .putLong(KEY_WORKER_SCHEDULED_TIME_ARG, System.nanoTime())
                .putString(KEY_STATS_ARG, jsonString)
                .putLong(KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        return Result.success(output);
    }

    private boolean downloadWeights(Context context, int projectId, int round) {

        PerformanceCheckpoint downloadWeightsPerformance = new PerformanceCheckpoint();

        // <-- Start
        downloadWeightsPerformance.start();

        NetworkManager networkManager = NetworkManager.getInstance(context);
        APIService service = networkManager.getApiService();
        Call<ResponseBody> call = service.downloadModel(projectId, round);

        Response<ResponseBody> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }

        if (!response.isSuccessful()) {
            return false;
        }

        ResponseBody body = response.body();
        if (body == null) {
            return false;
        }

        // End and report -->
        downloadWeightsPerformance.end();
        addProperty("download_weights_worker", "download_weights_duration", downloadWeightsPerformance.getDuration());

        PerformanceCheckpoint saveWeightsPerformance = new PerformanceCheckpoint();

        // <-- Start
        saveWeightsPerformance.start();

        // Save global model (replace if exists)
        String prefix = projectId + "_" + round + "_";
        File globalModelFile = new File(context.getFilesDir(), prefix + FLaaSLib.MODEL_WEIGHTS_FILENAME);
        if (globalModelFile.exists()) //noinspection ResultOfMethodCallIgnored
            globalModelFile.delete();
        boolean status = Utils.writeInputStreamToDisk(body.byteStream(), globalModelFile);

        // End and report -->
        saveWeightsPerformance.end();
        addProperty("download_weights_worker", "save_weights_duration", saveWeightsPerformance.getDuration());

        return status;
    }
}
