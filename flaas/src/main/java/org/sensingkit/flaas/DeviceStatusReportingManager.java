package org.sensingkit.flaas;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import org.sensingkit.flaas.workers.DeviceStatusWorker;

import java.util.concurrent.TimeUnit;

public class DeviceStatusReportingManager {

    @SuppressWarnings("unused")
    private static final String TAG = DeviceStatusReportingManager.class.getSimpleName();

    private static DeviceStatusReportingManager INSTANCE = null;

    // other instance variables can be here

    private DeviceStatusReportingManager() {}

    public static DeviceStatusReportingManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DeviceStatusReportingManager();
        }
        return(INSTANCE);
    }

    public void enable(Context context) {
        enableAlarm(context);
        enableBootReceiver(context);
    }

    public void disable(Context context) {
        disableAlarm(context);
        disableBootReceiver(context);

        // Cancel all FLaaS related workers
        WorkManager.getInstance(context).cancelAllWorkByTag("FLaaS");
    }

    private void enableAlarm(Context context) {
        PeriodicWorkRequest worker =
                new PeriodicWorkRequest.Builder(DeviceStatusWorker.class,
                        15, TimeUnit.MINUTES)
                        .setConstraints(Constraints.NONE)
                        .addTag("FLaaS")
                        .build();

        // add to the queue for execution
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DeviceStatusReporter",
                ExistingPeriodicWorkPolicy.KEEP,
                worker);
    }

    private void disableAlarm(Context context) {
        WorkManager.getInstance(context).cancelUniqueWork("DeviceStatusReporter");
    }

    private void enableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void disableBootReceiver(Context context) {
        ComponentName receiver = new ComponentName(context, DeviceBootReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }
}
