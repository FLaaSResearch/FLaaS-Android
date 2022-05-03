package org.sensingkit.flaas.rest;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.pushwoosh.Pushwoosh;

import org.sensingkit.flaas.network.APIService;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class NetworkManager {

    // Info here:
    // https://medium.com/@tsaha.cse/advanced-retrofit2-part-2-authorization-handling-ea1431cb86be

    private final Context context;
    private Session session;
    private APIService apiService;
    private AuthenticationListener authenticationListener;

    public static final String PREFS_NAME = "Settings";

    private static NetworkManager INSTANCE = null;

    public static NetworkManager getInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new NetworkManager(context);
        }
        return(INSTANCE);
    }

    private NetworkManager(Context context) {
        this.context = context;
    }

    public Session getSession() {

        if (session == null) {
            session = new Session() {
                @Override
                public boolean isLoggedIn() {
                    // check if token exist or not
                    // return true if exist otherwise false
                    // assuming that token exists
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    return prefs.contains("token");
                }

                @Override
                public void saveToken(String token) {
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("token", token);
                    editor.apply();
                }

                @Override
                public String getToken() {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String token = prefs.getString("token", "");
                    return token;
                }

                @Override
                public void saveRefreshToken(String token) {
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("refreshToken", token);
                    editor.apply();
                }

                @Override
                public String getRefreshToken() {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String token = prefs.getString("refreshToken", "");
                    return token;
                }

                @Override
                public void saveUsername(String username) {
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("username", username);
                    editor.apply();
                }

                @Override
                public String getUsername() {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String username = prefs.getString("username", "");
                    return username;
                }

                @Override
                public void savePassword(String password) {
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString("password", password);
                    editor.apply();
                }

                @Override
                public String getPassword() {
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    String password = prefs.getString("password", "");
                    return password;
                }

                @Override
                public void invalidate() {
                    // get called when user become logged out
                    // delete token and other user info from the storage
                    // (i.e: token, refreshToken) but not username (useful for user that wants to login again)
                    SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
                    editor.remove("token");
                    editor.remove("refreshToken");
                    editor.apply();

                    // Clear UserID from Pushwoosh
                    Pushwoosh.getInstance().setUserId("");
                    FirebaseCrashlytics.getInstance().setUserId("");

                    // sending logged out event to it's listener
                    // i.e: Activity, Fragment, Service
                    if (authenticationListener != null) {
                        authenticationListener.onUserLoggedOut();
                    }
                }
            };
        }

        return session;
    }

    public interface AuthenticationListener {
        void onUserLoggedOut();
    }

    public void setAuthenticationListener(AuthenticationListener listener) {
        this.authenticationListener = listener;
    }

    public APIService getApiService() {
        if (this.apiService == null) {
            this.apiService = provideRetrofit(APIService.URL).create(APIService.class);
        }
        return this.apiService;
    }

    private Retrofit provideRetrofit(String url) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(provideOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create(new Gson()))
                .build();
    }

    private OkHttpClient provideOkHttpClient() {
        OkHttpClient.Builder okhttpClientBuilder = new OkHttpClient.Builder();
        okhttpClientBuilder.followRedirects(true);
        okhttpClientBuilder.connectTimeout(30, TimeUnit.SECONDS);
        okhttpClientBuilder.readTimeout(2, TimeUnit.MINUTES);
        okhttpClientBuilder.writeTimeout(2, TimeUnit.MINUTES);
        okhttpClientBuilder.addInterceptor(new AuthInterceptor(getSession()));
        okhttpClientBuilder.addInterceptor(new TokenRenewInterceptor(getSession(), this));
        return okhttpClientBuilder.build();
    }
}
