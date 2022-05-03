package org.sensingkit.flaas;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.pushwoosh.Pushwoosh;

import org.sensingkit.flaas.rest.NetworkManager;
import org.sensingkit.flaas.workers.DownloadWeightsWorker;
import org.sensingkit.flaas.workers.SubmitResultsWorker;
import org.sensingkit.flaaslib.FLaaSLibBroadcastReceiver;
import org.sensingkit.flaaslib.enums.TrainingMode;
import org.sensingkit.flaaslib.workers.AbstractFLaaSWorker;
import org.sensingkit.flaaslib.workers.LocalTrainWorker;

public class MainActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    private static final String TAG = MainActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSIONS_REQUEST_CODE = 42;
    public static final int REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE = 43;

    private NetworkManager networkManager;
    private TextView textViewStatus;
    private Button authButton;

    ActivityResultLauncher<Intent> loginActivityLauncher;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // bind with UI
        this.textViewStatus = findViewById(R.id.status);
        TextView version = findViewById(R.id.version);
        this.authButton = findViewById(R.id.buttonAuthenticate);

        // Show version
        String versionName = BuildConfig.VERSION_NAME;
        version.setText("v" + versionName);

        this.loginActivityLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //Intent data = result.getData();
                        refreshUI();
                    }
                });

        // Init Network manager
        this.networkManager = NetworkManager.getInstance(this);

        // Register for Push Notifications
        Pushwoosh.getInstance().registerForPushNotifications(result -> {
            if (result.isSuccess()) {
                Log.d(TAG, "Successfully registered for push notifications with token: " + result.getData());

                // if logged in
                if (networkManager.getSession().isLoggedIn()) {

                    // set userId to Pushwoosh and Crashlytics
                    String username = networkManager.getSession().getUsername();
                    Log.d(TAG, "Saving username '" + username + "' as userId in Pushwoosh.");
                    Pushwoosh.getInstance().setUserId(username);
                    FirebaseCrashlytics.getInstance().setUserId(username);
                }
            } else {
                Log.e(TAG, "Failed to register for push notifications: " + result.getException().getMessage());
            }
        });

        // Update UI
        refreshUI();

        // Check for initial permission
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermission();
        }
        else {
            // request if needed
            requestBackgroundLocationPermission();
        }

        // Disable lib's BroadcastReceiver (temporary fix since we can't ignore the imported one from lib)
        PackageManager pm  = this.getPackageManager();
        ComponentName componentName = new ComponentName(this, FLaaSLibBroadcastReceiver.class);
        pm.setComponentEnabledSetting(componentName,PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        // TestBed Evaluation Only
        // parseIntent(getIntent());
    }

    protected void parseIntent(Intent intent) {
        String trainingMode = intent.getStringExtra("trainingMode");
        if (trainingMode == null) {
            Log.d(TAG, "Not a testbed, returning.");
            return;
        }

        int projectId = Integer.parseInt(intent.getStringExtra("projectId"));
        int round = Integer.parseInt(intent.getStringExtra("round"));
        int backendRequestId = 42;
        long validDate = System.currentTimeMillis() + 10 * 60 * 1000;  // 10 mins

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSIONS_REQUEST_CODE: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (!checkBackgroundLocationPermission()) {

                        requestBackgroundLocationPermission();
                    }
                }
            }
            break;
        }
    }

    public void showTNC(View view) {
        Intent intent = new Intent(this, TNCActivity.class);
        startActivity(intent);
    }

    public void showPrivacyPolicy(View view) {
        Intent intent = new Intent(this, PrivacyPolicyActivity.class);
        startActivity(intent);
    }

    public void showQuestionnaire(View view) {
        Intent intent = new Intent(this, QuestionnaireActivity.class);
        startActivity(intent);
    }

    /** Called when the user taps the Authenticate button */
    public void authenticate(View view) {

        if (networkManager.getSession().isLoggedIn()) {
            requestLogout();
        }
        else {
            Intent intent = new Intent(this, LoginActivity.class);
            this.loginActivityLauncher.launch(intent);
        }
    }

    private void showLoginScreen() {
        Intent intent = new Intent(this, LoginActivity.class);
        this.loginActivityLauncher.launch(intent);
    }

    private void requestLogout() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:

                    // Invalidate session
                    networkManager.getSession().invalidate();

                    // disable DeviceStatus reporting
                    DeviceStatusReportingManager.getInstance().disable(this);

                    // Cancel all queued jobs
                    WorkManager.getInstance(this).cancelAllWork();

                    // Now update the UI
                    refreshUI();
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    // No button clicked
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to logout?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @SuppressLint("SetTextI18n")
    private void refreshUI() {
        if (networkManager.getSession().isLoggedIn()) {

            // enable Logout
            this.textViewStatus.setText("You are Authenticated!");
            this.authButton.setText("Logout");
        }
        else {

            // enable Authenticate
            this.textViewStatus.setText("Please Authenticate...");
            this.authButton.setText("Authenticate");
        }
    }

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestPermissions(new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        }
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestPermissions(new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
        } else {
            requestPermissions(new String[]
                    {Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);

        }
    }

    private void requestBackgroundLocationPermission() {

        // ignore if < Q
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }

        // ignore if initial permission is not granted
        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (!checkBackgroundLocationPermission()) {
            new AlertDialog.Builder(this)
                    .setTitle("Background Location Permission")
                    .setMessage("Please allow location updates in background by choosing \"Allow all the time\" in next screen.")
                    .setPositiveButton("OK", (dialog, which) -> {
                        requestPermissions(new String[]
                                {Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_BACKGROUND_PERMISSIONS_REQUEST_CODE);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                    })
                    .create()
                    .show();
        }
    }

    public boolean checkBackgroundLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ActivityCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    // from https://blog.usejournal.com/building-an-app-usage-tracker-in-android-fe79e959ab26
//    private boolean checkForPermission() {
//        AppOpsManager appOps = (AppOpsManager) this.getSystemService(Context.APP_OPS_SERVICE);
//        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
//        return mode == AppOpsManager.MODE_ALLOWED;
//    }
}
