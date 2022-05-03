package org.sensingkit.flaas.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.sensingkit.flaas.DeviceInfo;
import org.sensingkit.flaas.network.APIService;
import org.sensingkit.flaas.rest.NetworkManager;

import java.io.IOException;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceStatusWorker extends Worker {

    private static final String TAG = DeviceStatusWorker.class.getSimpleName();

    public DeviceStatusWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        // get context
        Context context = getApplicationContext();

        // Report DeviceStatus
        Log.d(TAG, "Reporting DeviceStatus...");

        // get APIService
        NetworkManager networkManager = NetworkManager.getInstance(context);
        APIService service = networkManager.getApiService();

        // prepare response (in JSON)
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("request_type", "device-ping");
        JsonObject deviceInfo = DeviceInfo.getAllInfo(context);
        jsonObject.add("device_info", deviceInfo);

        // serialise
        String data = new Gson().toJson(jsonObject);  // Serialise to Json
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), data);

        // report availability
        Call<ResponseBody> reportAvailabilityCall = service.reportAvailability(body);
        reportAvailabilityCall.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                if (!response.isSuccessful()) {
                    ResponseBody body = response.errorBody();
                    if (body != null) {
                        try {
                            Log.e(TAG, body.string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    Log.d(TAG, "Succeed!");
                }
            }

            @Override
            public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                Log.e(TAG, t.getMessage());
            }
        });

        // return the result status
        return Result.success();
    }
}
