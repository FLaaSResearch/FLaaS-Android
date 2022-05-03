package org.sensingkit.flaas;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pushwoosh.notification.NotificationServiceExtension;
import com.pushwoosh.notification.PushMessage;

import org.sensingkit.flaas.rest.NetworkManager;
import org.sensingkit.flaas.workers.DownloadWeightsWorker;
import org.sensingkit.flaas.workers.SubmitResultsWorker;
import org.sensingkit.flaaslib.enums.TrainingMode;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;
import org.sensingkit.flaaslib.workers.LocalTrainWorker;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@SuppressWarnings("unused")
public class FLaaSNotificationService  extends NotificationServiceExtension {

    private static final String TAG = FLaaSNotificationService.class.getSimpleName();

    private static Map<String, String> jsonConverter(String jsonString) {
        return new Gson().fromJson(
                jsonString, new TypeToken<HashMap<String, String>>() {}.getType()
        );
    }

    @Override
    public boolean onMessageReceived(final PushMessage message) {

        assert getApplicationContext() != null;
        Handler mainHandler = new Handler(getApplicationContext().getMainLooper());
        mainHandler.post(() -> handlePush(message));

        // Display notification?
        return false;
    }

    @Override
    protected void startActivityForPushMessage(PushMessage message) {
        super.startActivityForPushMessage(message);
        Log.d(TAG, ">>> startActivityForPushMessage");
        //handlePush(message);
    }

    @MainThread
    private void handlePush(final PushMessage message) {
        Log.d(TAG, "NotificationService.handlePush: " + message.toJson().toString());

        // Get custom data
        String dataString = message.getCustomData();
        if (dataString == null) {
            Log.d(TAG, "Data not available. Ignoring.");
            return;
        }

        // Convert to Map
        Map<String, String> data = jsonConverter(dataString);
        if (data == null) {
            Log.d(TAG, "Data could not be decoded. Ignoring.");
            return;
        }

        // Get Command
        String type = data.get("type");
        long validDate = Long.parseLong(Objects.requireNonNull(data.get("validDate")));
        int backendRequestId = Integer.parseInt(Objects.requireNonNull(data.get("request")));
        int projectId = Integer.parseInt(Objects.requireNonNull(data.get("project")));
        int round = Integer.parseInt(Objects.requireNonNull(data.get("round")));
        String trainingMode = data.get("trainingMode");

        if (type == null) {
            Log.d(TAG, "Key 'command' cannot be null. Ignoring.");
            return;
        }

        // Forward command
        switch (type) {

            case "train":
                handleTrain(backendRequestId, projectId, round, trainingMode, validDate);
                break;

            default:
                Log.e(TAG, "Unsupported command '" + type + "'. Ignoring.");
        }
    }

    private void handleTrain(int backendRequestId, int projectId, int round, String trainingMode, long validDate) {
        // Execute worker
        // info: https://developer.android.com/codelabs/android-workmanager-java

        // prepare context
        Context context = getApplicationContext();
        assert context != null;

        NetworkManager manager = NetworkManager.getInstance(context);
        String username = manager.getSession().getUsername();

        long receivedTime = System.nanoTime();
        long receivedLocalTime = System.currentTimeMillis();

        TrainingMode mode = TrainingMode.fromValue(trainingMode);
        switch (mode) {
            case BASELINE:
                baselineTraining(context, backendRequestId, projectId, round, trainingMode, username, receivedTime, receivedLocalTime, validDate);
                break;
            case JOINT_MODELS:
                jointModelsTraining(context, backendRequestId, projectId, round, trainingMode, username, receivedTime, receivedLocalTime, validDate);
                break;
            case JOINT_SAMPLES:
                jointSamplesTraining(context, backendRequestId, projectId, round, trainingMode, username, receivedTime, receivedLocalTime, validDate);
                break;
            default:
                Log.e(TAG, "Unknown training mode: " + mode);
        }
    }

    private void baselineTraining(Context context, int backendRequestId, int projectId, int round, String trainingMode, String username, long receivedTime, long receivedLocalTime, long validDate) {

        // prepare data input
        Data inputData = new Data.Builder()
                .putInt(AbstractFLaaSWorker.KEY_BACKEND_REQUEST_ID_ARG, backendRequestId)
                .putInt(AbstractFLaaSWorker.KEY_PROJECT_ID_ARG, projectId)
                .putInt(AbstractFLaaSWorker.KEY_ROUND_ARG, round)
                .putString(AbstractFLaaSWorker.KEY_TRAINING_MODE_ARG, trainingMode)
                .putString(AbstractFLaaSWorker.KEY_USERNAME_ARG, username)
                .putLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, receivedTime)
                .putLong(AbstractFLaaSWorker.KEY_LOCAL_TIME_ARG, receivedLocalTime)
                .putLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        // create workers
        OneTimeWorkRequest downloadWeightsWorker = new OneTimeWorkRequest.Builder(DownloadWeightsWorker.class)
                .setInputData(inputData)
                .addTag("FLaaS")
                .build();

        OneTimeWorkRequest localTrainingWorker = new OneTimeWorkRequest.Builder(LocalTrainWorker.class)
                .addTag("FLaaS")
                .build();

        OneTimeWorkRequest submitResultsWorker = new OneTimeWorkRequest.Builder(SubmitResultsWorker.class)
                .addTag("FLaaS")
                .build();

        // add to the queue for execution
        WorkManager.getInstance(context)
                .beginWith(downloadWeightsWorker)
                .then(localTrainingWorker)
                .then(submitResultsWorker)
                .enqueue();
    }

    private void jointModelsTraining(Context context, int backendRequestId, int projectId, int round, String trainingMode, String username, long receivedTime, long receivedLocalTime, long validDate) {

        // prepare data input
        Data inputData = new Data.Builder()
                .putInt(AbstractFLaaSWorker.KEY_BACKEND_REQUEST_ID_ARG, backendRequestId)
                .putInt(AbstractFLaaSWorker.KEY_PROJECT_ID_ARG, projectId)
                .putInt(AbstractFLaaSWorker.KEY_ROUND_ARG, round)
                .putString(AbstractFLaaSWorker.KEY_TRAINING_MODE_ARG, trainingMode)
                .putString(AbstractFLaaSWorker.KEY_USERNAME_ARG, username)
                .putLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, receivedTime)
                .putLong(AbstractFLaaSWorker.KEY_LOCAL_TIME_ARG, receivedLocalTime)
                .putLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        // create workers
        OneTimeWorkRequest downloadWeightsWorker = new OneTimeWorkRequest.Builder(DownloadWeightsWorker.class)
                .setInputData(inputData)
                .addTag("FLaaS")
                .build();

        // add to the queue for execution
        WorkManager.getInstance(context).enqueue(downloadWeightsWorker);
    }

    private void jointSamplesTraining(Context context, int backendRequestId, int projectId, int round, String trainingMode, String username, long receivedTime, long receivedLocalTime, long validDate) {

        // prepare data input
        Data inputData = new Data.Builder()
                .putInt(AbstractFLaaSWorker.KEY_BACKEND_REQUEST_ID_ARG, backendRequestId)
                .putInt(AbstractFLaaSWorker.KEY_PROJECT_ID_ARG, projectId)
                .putInt(AbstractFLaaSWorker.KEY_ROUND_ARG, round)
                .putString(AbstractFLaaSWorker.KEY_TRAINING_MODE_ARG, trainingMode)
                .putString(AbstractFLaaSWorker.KEY_USERNAME_ARG, username)
                .putLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, receivedTime)
                .putLong(AbstractFLaaSWorker.KEY_LOCAL_TIME_ARG, receivedLocalTime)
                .putLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, validDate)
                .build();

        // create workers
        OneTimeWorkRequest downloadWeightsWorker = new OneTimeWorkRequest.Builder(DownloadWeightsWorker.class)
                .setInputData(inputData)
                .addTag("FLaaS")
                .build();

        // add to the queue for execution
        WorkManager.getInstance(context).enqueue(downloadWeightsWorker);
    }

}
