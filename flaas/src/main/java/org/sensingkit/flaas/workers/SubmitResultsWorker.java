package org.sensingkit.flaas.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.sensingkit.flaas.DeviceInfo;
import org.sensingkit.flaas.network.APIService;
import org.sensingkit.flaas.rest.NetworkManager;
import org.sensingkit.flaaslib.FLaaSLib;
import org.sensingkit.flaaslib.enums.JoinedRoundStatus;
import org.sensingkit.flaaslib.utils.Utils;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class SubmitResultsWorker extends AbstractNetworkFLaaSWorker {

    public static final String RESULTS_FILENAME = "performance.json";

    @SuppressWarnings("unused")
    private static final String TAG = SubmitResultsWorker.class.getSimpleName();

    public SubmitResultsWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d(TAG, "SubmitResultsWorker is executing...");

        // prepare failure result
        Result failureResult = getFailureResult();

        // get context
        Context context = getApplicationContext();

        // get input data
        int backendRequestID = getInputData().getInt(KEY_BACKEND_REQUEST_ID_ARG, -1);
        int projectId = getInputData().getInt(KEY_PROJECT_ID_ARG, -1);
        int round = getInputData().getInt(KEY_ROUND_ARG, -1);
        long receivedTime = getInputData().getLong(AbstractFLaaSWorker.KEY_WORKER_SCHEDULED_TIME_ARG, -1);
        long validDate = getInputData().getLong(AbstractFLaaSWorker.KEY_REQUEST_VALID_DATE_ARG, -1);

        // If not valid, just return Failure (not retry)
        if (!isTaskValid(validDate)) {
            Log.d(TAG, "Task is not valid anymore and will be skipped.");
            joinRound(context, projectId, round, JoinedRoundStatus.SUBMIT_RESULTS);
            return Result.failure();
        }

        // init stats
        String statsJson = getInputData().getString(KEY_STATS_ARG);
        loadStats(statsJson);

        // add attempt counter
        addProperty("submit_results_worker", "attempt", getRunAttemptCount());

        // add worker started after X secs
        float workerStarted = (float)((System.nanoTime() - receivedTime) / 1e9);
        addProperty("submit_results_worker", "worker_started_after", workerStarted);

        // load weights and submit them
        Log.d(TAG, "Submitting weights...");
        if (!submitWeights(context, projectId, round)) {
            Log.e(TAG, "Submit weights failed.");
            return failureResult;
        }

        // report availability
        // TODO: Disable block for TestBed Evaluations
        Log.d(TAG, "Reporting availability...");
        if (!reportAvailability(context, backendRequestID)) {
            Log.e(TAG, "Report availability failed.");
            return failureResult;
        }

        // compute total time (end delete notification_received_time)
        long startTime = this.stats.getAsJsonObject("general").get("notification_received_time").getAsLong();
        float totalDuration = (float)((System.nanoTime() - startTime) / 1e9);
        addProperty("general", "total_duration", totalDuration);
        this.stats.getAsJsonObject("general").remove("notification_received_time");

        // submit stats (times, loss etc.)
        Log.d(TAG, "Submitting stats...");
        if (!submitStats(context, projectId, round)) {
            Log.e(TAG, "Submit stats failed.");
            return failureResult;
        }

        // delete weights
        String prefix = projectId + "_" + round + "_";
        File globalModelFile = new File(context.getFilesDir(), prefix + FLaaSLib.MODEL_WEIGHTS_FILENAME);
        if (globalModelFile.exists()) //noinspection ResultOfMethodCallIgnored
            globalModelFile.delete();

        return Result.success();
    }

    private boolean submitWeights(Context context, int projectId, int round) {


        String prefix = projectId + "_" + round + "_";
        File globalModelFile = new File(context.getFilesDir(), prefix + FLaaSLib.MODEL_WEIGHTS_FILENAME);
        byte[] weights;
        try {
            weights = Utils.loadData(globalModelFile);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }

        NetworkManager networkManager = NetworkManager.getInstance(context);
        APIService service = networkManager.getApiService();
        Call<ResponseBody> call = service.submitModel(
                projectId,
                round,
                FLaaSLib.MODEL_WEIGHTS_FILENAME,
                RequestBody.create(MediaType.parse("application/octet-stream"), weights));

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
        return body != null;
    }

    private boolean reportAvailability(Context context, int requestId) {

        // prepare response (in JSON)
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("request_type", "device-train");
        jsonObject.addProperty("request_id", requestId);
        JsonObject deviceInfo = DeviceInfo.getAllInfo(context);
        jsonObject.add("device_info", deviceInfo);

        // serialise
        String data = new Gson().toJson(jsonObject);  // Serialise to Json
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), data);

        NetworkManager networkManager = NetworkManager.getInstance(context);
        APIService service = networkManager.getApiService();
        Call<ResponseBody> call = service.reportAvailability(body);

        try {
            Response<ResponseBody> response = call.execute();
            // check response if needed

            if (!response.isSuccessful()) {
                Log.e(TAG, response.errorBody() != null ? response.errorBody().string() : "Response not successful");
                return false;
            }
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().recordException(e);
            return false;
        }

        return true;
    }

    private boolean submitStats(Context context, int projectId, int round) {

        String jsonData = getStatsInJsonString();

        // Submit all stats
        if (!submitJsonData(context, projectId, round, jsonData, RESULTS_FILENAME)) {
            Log.e(TAG, "Submit stats failed.");
            return false;
        }

        return true;
    }

    private boolean submitJsonData(Context context, int projectId, int round, String jsonData, @SuppressWarnings("SameParameterValue") String filename) {
        boolean result = true;

        NetworkManager networkManager = NetworkManager.getInstance(context);
        APIService service = networkManager.getApiService();
        Call<ResponseBody> call = service.submitResults(
                projectId,
                round,
                filename,
                RequestBody.create(MediaType.parse("application/json"), jsonData));

        Response<ResponseBody> response = null;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
            result = false;
        }
        if (result) {
            if (!response.isSuccessful()) {
                result = false;
            } else {
                ResponseBody body = response.body();
                if (body == null) {
                    result = false;
                }
            }

        }

        return result;
    }
}
