package org.sensingkit.redapp;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.sensingkit.flaaslib.FLaaSLib;
import org.sensingkit.flaaslib.enums.App;
import org.sensingkit.flaaslib.enums.DatasetType;

import java.io.File;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int CHECK_DELAY_MS = 5000;

    private Button mSetupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI
        this.mSetupButton = findViewById(R.id.buttonSetup);

        // Show version
        String versionName = BuildConfig.VERSION_NAME;
        TextView version = findViewById(R.id.version);
        version.setText("v" + versionName);

        // Check status of button
        checkStatus(true);
    }

    public void setupApp(View view) {

        // Check if FLaaS is installed
        if (!FLaaSLib.isAppInstalled(this, App.FLAAS)) {
            Log.e(TAG, "FLaaS app is not installed");
            showError("FLaaS app in not installed", "Please install FLaaS app before you attempt to setup this app.");
            return;
        }

        mSetupButton.setText("Please Wait...");
        mSetupButton.setEnabled(false);

        // Request both datasets (-1 since not relevant to this case)
        FLaaSLib.requestSamples(this, App.FLAAS, "CIFAR10", DatasetType.IID, -1, -1);
        FLaaSLib.requestSamples(this, App.FLAAS, "CIFAR10", DatasetType.NON_IID, -1, -1);

        // Check after 5 seconds
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> checkStatus(false), CHECK_DELAY_MS);
    }

    private void checkStatus(boolean onCreate) {

        // declare the two files
        File file1 = new File(getFilesDir(), DatasetType.IID.getFilename());
        File file2 = new File(getFilesDir(), DatasetType.NON_IID.getFilename());

        if (file1.exists() && file2.exists()) {
            mSetupButton.setText("App is ready!");
            mSetupButton.setEnabled(false);
        }
        else {
            if (!onCreate) {
                showError("Communication Error", "Something went wrong while communicating with FLaaS app. Please request to setup this app again. If the error persists, make sure you have FLaaS app installed. You can also try to restart your device.");
            }

            mSetupButton.setText("SETUP APP");
            mSetupButton.setEnabled(true);
        }
    }

    private void showError(String title, String message) {
        AlertDialog alert = new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, id) -> {
                    //do things
                })
                .create();
        alert.show();
    }
}