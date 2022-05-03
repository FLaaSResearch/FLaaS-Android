package org.sensingkit.flaas.rest;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {

    private static final String TAG = AuthInterceptor.class.getSimpleName();
    private Session session;

    public AuthInterceptor(Session session) {
        this.session = session;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {

        Request originalRequest = chain.request();

        if (session.isLoggedIn()) {

            // Request customization: add request headers
            Request.Builder requestBuilder = originalRequest.newBuilder()
                    .header("Authorization", getAuthorizationHeader());

            Request request = requestBuilder.build();

            return chain.proceed(request);
        }

        return chain.proceed(originalRequest);
    }

    public String getAuthorizationHeader() {
        return "Bearer " + this.session.getToken();
    }
}
