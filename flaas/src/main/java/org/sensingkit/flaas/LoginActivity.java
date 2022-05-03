package org.sensingkit.flaas;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.pushwoosh.Pushwoosh;

import org.sensingkit.flaas.model.RetroToken;
import org.sensingkit.flaas.network.APIService;
import org.sensingkit.flaas.rest.NetworkManager;
import org.sensingkit.flaas.rest.Session;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class LoginActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private static final String TAG = LoginActivity.class.getSimpleName();

    TextInputEditText usernameEditText;
    TextInputEditText passwordEditText;

    NetworkManager networkManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // bind with UI
        this.usernameEditText = findViewById(R.id.username_edit_text);
        this.passwordEditText = findViewById(R.id.password_edit_text);
        this.passwordEditText.setImeActionLabel("Login", KeyEvent.KEYCODE_ENTER);
        this.passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                login();
                handled = true;
            }
            return handled;
        });

        // init NetworkManager
        this.networkManager = NetworkManager.getInstance(this);

        // Set email (if exists)
        String email = this.networkManager.getSession().getUsername();
        this.usernameEditText.setText(email);

        // Set password (if exists)
        String password = this.networkManager.getSession().getPassword();
        this.passwordEditText.setText(password);
    }

    /** Called when the user taps the Cancel button */
    public void cancel(View view) {
        finish();
    }

    /** Called when the user taps the Login button */
    public void login(View view) {
        login();
    }

    private void login() {
        String username = this.usernameEditText.getText().toString();
        String password = this.passwordEditText.getText().toString();

        APIService service = networkManager.getApiService();
        Call<RetroToken> call = service.authenticate(username, password);
        call.enqueue(new Callback<RetroToken>() {
            @Override
            public void onResponse(Call<RetroToken> call, Response<RetroToken> response) {
                if (response.isSuccessful()) {
                    authenticated(response.body());
                }
                else {
                    unsuccessful(response);
                }
            }

            @Override
            public void onFailure(Call<RetroToken> call, Throwable t) {
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
                Log.d(TAG, t.getMessage());
            }
        });
    }

    private void authenticated(RetroToken data) {
        Session session = this.networkManager.getSession();
        session.saveToken(data.getAccess());
        Log.d(TAG, "Token is: "+ data.getAccess());
        session.saveRefreshToken(data.getRefresh());

        // save Username into a. local store, b. Pushwoosh, c. Crashlytics
        String username = this.usernameEditText.getText().toString();
        session.saveUsername(username);

        Log.d(TAG, "Saving username '" + username + "' as userId in Pushwoosh.");
        Pushwoosh.getInstance().setUserId(username);

        FirebaseCrashlytics.getInstance().setUserId(username);

        Log.d(TAG, "Successfully LoggedIn");
        setResult(RESULT_OK, null);

        // save password
        String password = this.passwordEditText.getText().toString();
        session.savePassword(password);

        // Download both IID and NonIID samples for RGB
        downloadSamples(App.RED, DatasetType.IID);
        downloadSamples(App.RED, DatasetType.NON_IID);
        downloadSamples(App.GREEN, DatasetType.IID);
        downloadSamples(App.GREEN, DatasetType.NON_IID);
        downloadSamples(App.BLUE, DatasetType.IID);
        downloadSamples(App.BLUE, DatasetType.NON_IID);

        // enable DeviceStatus reporting
        DeviceStatusReportingManager.getInstance().enable(this);
    }

    private void unsuccessful(Response response) {

        if (response.code() == 401) {
            // Not authorized
            Toast.makeText(getApplicationContext(), "The combination of Username and Password is incorrect. Please try again. ", Toast.LENGTH_LONG).show();
        }
        else {
            String message = response.code() + ": " + response.message();
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }

    private void downloadSamples(App app, DatasetType datasetType) {
        APIService service = networkManager.getApiService();
        Call<ResponseBody> call = service.getSamples(datasetType.getName(), app.getId());

        call.enqueue(new Callback<ResponseBody>() {

            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                if (response.isSuccessful()) {

                    // prepare filename (e.g., Red_samples_iid.bin)
                    String filename = app.getName() + "_" + datasetType.getFilename();

                    // download samples
                    samplesDownloaded(response.body(), filename);
                }
                else {
                    Log.e(TAG, "Response from downloadSamples() was not successful.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Call downloadSamples() has failed.");
            }
        });
    }

    private void samplesDownloaded(ResponseBody body, String filename) {
        // Save global model (replace if exists)
        File samplesFile = new File(this.getFilesDir(), filename);
        if (samplesFile.exists()) samplesFile.delete();
        if (!writeResponseBodyToDisk(body, samplesFile)) {
            Log.e(TAG, "Error while writing samples to disk.");
        }

        Log.d(TAG, "Samples Downloaded and saved at " + filename);

        finish();  // FINISHED
    }

    private static boolean writeResponseBodyToDisk(ResponseBody body, File file) {

        try {
            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                //long fileSize = body.contentLength();
                //long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    //fileSizeDownloaded += read;
                }

                outputStream.flush();
                return true;

            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}