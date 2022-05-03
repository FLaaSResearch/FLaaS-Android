package org.sensingkit.flaaslib.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.Random;

public abstract class AbstractFLaaSWorker extends Worker {

    @SuppressWarnings("unused")
    private static final String TAG = AbstractFLaaSWorker.class.getSimpleName();

    public static final String KEY_REQUEST_VALID_DATE_ARG = "REQUEST_SENT_DATETIME";
    public static final String KEY_BACKEND_REQUEST_ID_ARG = "BACKEND_REQUEST_ID";
    public static final String KEY_REQUEST_ID_ARG = "REQUEST_ID";
    public static final String KEY_PROJECT_ID_ARG = "PROJECT_ID";
    public static final String KEY_ROUND_ARG = "ROUND_ID";
    public static final String KEY_USERNAME_ARG = "USERNAME_ID";
    public static final String KEY_DATASET_ARG = "DATASET_ID";
    public static final String KEY_DATASET_TYPE_ARG = "DATASET_TYPE_ID";
    public static final String KEY_MODEL_ARG = "MODEL_ID";
    public static final String KEY_EPOCHS_ARG = "EPOCHS_ID";
    public static final String KEY_SEED_ARG = "SEED_ID";
    public static final String KEY_MAX_SAMPLES_ARG = "MAX_SAMPLES_ID";
    public static final String KEY_TRAINING_MODE_ARG = "TRAINING_MODE_ID";

    public static final String KEY_STATS_ARG = "STATS_ID";
    public static final String KEY_APP_STATS_ARG = "APP_STATS_ID";
    public static final String KEY_DURATIONS_ARG = "DURATIONS_ID";
    public static final String KEY_WORKER_SCHEDULED_TIME_ARG = "WORKER_SCHEDULED_TIME_ID";
    public static final String KEY_LOCAL_TIME_ARG = "LOCAL_TIME_ID";

    public static final int FAILURE_RETRY = 3;
    //TODO: Change to '1' for TestBed Evaluations

    protected Random random;
    protected JsonObject stats;

    public AbstractFLaaSWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected Result getFailureResult() {
        Result failureResult;
        if (getRunAttemptCount() < FAILURE_RETRY) {
            failureResult = Result.retry();
        }
        else {
            failureResult = Result.failure();
        }
        return failureResult;
    }

    protected static boolean isTaskValid(long validDate) {
        long localTime = System.currentTimeMillis();
        return validDate >= localTime;
    }

    protected static Random createRandom(int seed, int round, String username) {
        int customSeed = seed + round + username.hashCode();
        return new Random(customSeed);
    }

    protected void loadStats(String json) {

        if (json != null) {
            this.stats = new Gson().fromJson(json, JsonObject.class);
        }
        else {
            this.stats = new JsonObject();
        }
    }

    @SuppressWarnings("SameParameterValue")
    protected void addJsonArray(String member, String property, JsonArray array) {
        JsonObject memberObject = this.stats.getAsJsonObject(member);
        if (memberObject == null) {
            memberObject = new JsonObject();
        }
        memberObject.add(property, array);

        this.stats.add(member, memberObject);
    }

    @SuppressWarnings("SameParameterValue")
    protected void addJsonObject(String member, String property, JsonObject object) {
        JsonObject memberObject = this.stats.getAsJsonObject(member);
        if (memberObject == null) {
            memberObject = new JsonObject();
        }
        memberObject.add(property, object);

        this.stats.add(member, memberObject);
    }

    protected void addProperty(String member, String property, Number value) {

        JsonObject memberObject = this.stats.getAsJsonObject(member);
        if (memberObject == null) {
            memberObject = new JsonObject();
        }
        memberObject.addProperty(property, value);

        this.stats.add(member, memberObject);
    }

    protected void addProperty(String member, String property, String value) {

        JsonObject memberObject = this.stats.getAsJsonObject(member);
        if (memberObject == null) {
            memberObject = new JsonObject();
        }
        memberObject.addProperty(property, value);

        this.stats.add(member, memberObject);
    }

    protected String getStatsInJsonString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this.stats);
    }
}
