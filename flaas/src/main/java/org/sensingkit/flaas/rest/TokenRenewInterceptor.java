package org.sensingkit.flaas.rest;

import android.util.Log;

import org.sensingkit.flaas.model.RetroToken;
import org.sensingkit.flaas.network.APIService;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class TokenRenewInterceptor implements Interceptor {

    private static final String TAG = TokenRenewInterceptor.class.getSimpleName();
    private final Session session;
    private final NetworkManager networkManager;

    public TokenRenewInterceptor(Session session, NetworkManager networkManager) {
        this.session = session;
        this.networkManager = networkManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();

        // execute original request
        Response response = chain.proceed(originalRequest);

        if (response.code() == 401) {  // if unauthorized / forbidden (token expired)
            synchronized (this.networkManager) {

                //Log.d(TAG, "Refreshing expired token: " + session.getToken());

                APIService service = networkManager.getApiService();
                String refreshToken = session.getRefreshToken();
                retrofit2.Response<RetroToken> tokenResponse = service.refreshToken(refreshToken).execute();

                if (tokenResponse.isSuccessful()) {

                    response.close();

                    // save new token
                    RetroToken data = tokenResponse.body();
                    session.saveToken(data.getAccess());
                    session.saveRefreshToken(data.getRefresh());
                    Log.d(TAG, "Refreshed token: " + data.getAccess());

                    // retry previous request but with new token
                    Request.Builder builder = originalRequest.newBuilder().
                            header("Authorization", getAuthorizationHeader()).
                            method(originalRequest.method(), originalRequest.body());
                    response = chain.proceed(builder.build());
                }
                else {
                    // Something went wrong!
                    // We tried to refresh but didn't work. Maybe refresh token also expired?
                    session.invalidate();
                }
            }
        }

        return response;
    }

    public String getAuthorizationHeader() {
        return "Bearer " + this.session.getToken();
    }
}
