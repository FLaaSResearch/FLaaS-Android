package org.sensingkit.flaas.workers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.WorkerParameters;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.sensingkit.flaas.model.RetroRound;
import org.sensingkit.flaas.network.APIService;
import org.sensingkit.flaas.rest.NetworkManager;
import org.sensingkit.flaaslib.enums.JoinedRoundStatus;
import org.sensingkit.flaaslib.utils.PerformanceCheckpoint;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Response;

public abstract class AbstractNetworkFLaaSWorker extends AbstractFLaaSWorker {

    public AbstractNetworkFLaaSWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    protected RetroRound joinRound(Context context, int projectId, int round, JoinedRoundStatus status) {

        NetworkManager networkManager = NetworkManager.getInstance(context);
        APIService service = networkManager.getApiService();
        Call<RetroRound> call = service.joinRound(projectId, round, status.getId());

        Response<RetroRound> response;
        try {
            response = call.execute();
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
            return null;
        }

        if (!response.isSuccessful()) {
            return null;
        }

        return response.body();
    }
}
