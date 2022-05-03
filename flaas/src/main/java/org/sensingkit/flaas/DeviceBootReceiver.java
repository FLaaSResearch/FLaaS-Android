package org.sensingkit.flaas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.sensingkit.flaas.rest.NetworkManager;

public class DeviceBootReceiver extends BroadcastReceiver {

    private static final String TAG = DeviceBootReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {

            // Check if LoggedIn
            NetworkManager networkManager = NetworkManager.getInstance(context);
            if (networkManager.getSession().isLoggedIn()) {

                // Enable the Device Status reporter
                DeviceStatusReportingManager.getInstance().enable(context);
                Log.d(TAG, "Device Status Reporting is now enabled");
            }
            else {
                Log.d(TAG, "User is not logged in, will not enable Device Status Reporting");
            }
        }
    }
}
