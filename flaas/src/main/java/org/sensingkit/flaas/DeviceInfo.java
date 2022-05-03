package org.sensingkit.flaas;

import android.Manifest;
import android.app.ActivityManager;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.StatFs;
import android.telephony.CellIdentity;
import android.telephony.CellInfo;
import android.telephony.CellSignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.util.List;

import static android.content.Context.USAGE_STATS_SERVICE;

public class DeviceInfo {

    @SuppressWarnings("unused")
    private static final String TAG = DeviceInfo.class.getSimpleName();

    public static JsonObject getAllInfo(Context context) {

        JsonObject jsonObject = new JsonObject();

        try {
            JsonObject appDetails = getAppDetails(context);
            jsonObject.add("app_details", appDetails);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve appDetails");
        }

        try {
            JsonObject batteryStatus = getBatteryStatus(context);
            jsonObject.add("battery_status", batteryStatus);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve batteryStatus");
        }

        try {
            JsonObject connectivityStatus = getConnectivityStatus(context);
            jsonObject.add("connectivity_status", connectivityStatus);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve connectivityStatus");
        }

        try {
            JsonObject memoryInfo = getMemoryInfo(context);
            jsonObject.add("memory_info", memoryInfo);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve getMemoryInfo");
        }

        try {
            JsonObject diskInfo = getDisksInfo(context);
            jsonObject.add("disks_info", diskInfo);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve diskInfo");
        }

        try {
            JsonObject deviceDetails = getDeviceDetails(context);
            jsonObject.add("device_details", deviceDetails);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve deviceDetails");
        }

        try {
            JsonObject cellTowersInfo = getCellTowersInfo(context);
            jsonObject.add("cell_towers_info", cellTowersInfo);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve cellTowersInfo");
        }

        try {
            JsonObject location = getLocation(context);
            jsonObject.add("location", location);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve location");
        }

        try {
            JsonObject usageStatsDetails = getUsageStatsDetails(context);
            jsonObject.add("usage_stats_details", usageStatsDetails);
        } catch (Exception ex) {
            Log.e(TAG, "Unable to retrieve appDetails");
        }

        return jsonObject;
    }

    public static JsonObject getAppDetails(Context context) {

        JsonObject jsonObject = new JsonObject();

        int versionCode = BuildConfig.VERSION_CODE;
        jsonObject.addProperty("version_code", versionCode);

        String versionName = BuildConfig.VERSION_NAME;
        jsonObject.addProperty("version_name", versionName);

        return jsonObject;
    }

    public static JsonObject getBatteryStatus(Context context) {

        JsonObject jsonObject = new JsonObject();

        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        float batteryPct = level / (float) scale;
        jsonObject.addProperty("level", batteryPct);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        jsonObject.addProperty("status", status);

        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);
        jsonObject.addProperty("health", health);

        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        jsonObject.addProperty("plugged_status", chargePlug);

        boolean low = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            low = batteryStatus.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false);
        }
        jsonObject.addProperty("battery_low", low);

        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        jsonObject.addProperty("voltage", voltage);

        int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        jsonObject.addProperty("temperature", temperature);

        String technology = batteryStatus.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
        jsonObject.addProperty("technology", technology);

        boolean powerPlugged = (chargePlug == BatteryManager.BATTERY_PLUGGED_AC || chargePlug == BatteryManager.BATTERY_PLUGGED_USB || chargePlug == BatteryManager.BATTERY_PLUGGED_WIRELESS);
        jsonObject.addProperty("power_plugged", powerPlugged);

        return jsonObject;
    }

    public static JsonObject getConnectivityStatus(Context context) {

        JsonObject jsonObject = new JsonObject();

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        boolean isActiveNetworkMetered = cm.isActiveNetworkMetered();
        jsonObject.addProperty("is_active_network_metered", isActiveNetworkMetered);

        boolean isDefaultNetworkActive = cm.isDefaultNetworkActive();
        jsonObject.addProperty("is_default_network_active", isDefaultNetworkActive);

        int restrictBackground = cm.getRestrictBackgroundStatus();
        jsonObject.addProperty("restrict_background_status", restrictBackground);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        jsonObject.add("active_network", getNetworkInfo(context, activeNetwork));

        JsonArray networksArray = new JsonArray();
        Network[] allNetworks = cm.getAllNetworks();
        for (Network network : allNetworks) {
            if (network == null) continue;

            NetworkInfo networkInfo = cm.getNetworkInfo(network);
            if (networkInfo == null) continue;

            JsonObject networkInfoJson = getNetworkInfo(context, networkInfo);
            networksArray.add(networkInfoJson);
        }
        jsonObject.add("all_networks", networksArray);

        return jsonObject;
    }

    public static JsonObject getNetworkInfo(Context context, NetworkInfo info) {

        JsonObject jsonObject = new JsonObject();

        NetworkInfo.State state = info.getState();
        jsonObject.addProperty("state", state.name());

        NetworkInfo.DetailedState detailedState = info.getDetailedState();
        jsonObject.addProperty("detailed_state", detailedState.name());

        String extraInfo = info.getExtraInfo();
        jsonObject.addProperty("extra_info", extraInfo);

        String typeName = info.getTypeName();
        jsonObject.addProperty("type_name", typeName);

        int type = info.getType();
        jsonObject.addProperty("type", type);

        String subtypeName = info.getSubtypeName();
        jsonObject.addProperty("subtype_name", subtypeName);

        int subtype = info.getSubtype();
        jsonObject.addProperty("subtype", subtype);

        boolean isConnectedOrConnecting = info.isConnectedOrConnecting();
        jsonObject.addProperty("is_connected_or_connecting", isConnectedOrConnecting);

        boolean isConnected = info.isConnected();
        jsonObject.addProperty("is_connected", isConnected);

        boolean isRoaming = info.isRoaming();
        jsonObject.addProperty("is_roaming", isRoaming);

        boolean isAvailable = info.isAvailable();
        jsonObject.addProperty("is_available", isAvailable);

        boolean isFailover = info.isFailover();
        jsonObject.addProperty("is_failover", isFailover);

        return jsonObject;
    }

    public static JsonObject getMemoryInfo(Context context) {
        JsonObject jsonObject = new JsonObject();

        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(memInfo);

        long totalMemory = memInfo.totalMem;
        jsonObject.addProperty("total_memory", totalMemory);

        long availMemory = memInfo.availMem;
        jsonObject.addProperty("available_memory", availMemory);

        long threshold = memInfo.threshold;
        jsonObject.addProperty("threshold", threshold);

        Boolean lowMemory = memInfo.lowMemory;
        jsonObject.addProperty("low_memory", lowMemory);

        return jsonObject;
    }

    public static JsonObject getDisksInfo(Context context) {
        JsonObject jsonObject = new JsonObject();

        File internalStorageFile = context.getFilesDir();
        jsonObject.add("internal", getDiskInfo(internalStorageFile));

        File[] externalStorageFiles = ContextCompat.getExternalFilesDirs(context, null);
        JsonArray disksArray = new JsonArray();
        for (File externalStorageFile : externalStorageFiles) {
            disksArray.add(getDiskInfo(externalStorageFile));
        }
        jsonObject.add("external", disksArray);

        return jsonObject;
    }

    private static JsonObject getDiskInfo(File diskPath) {
        JsonObject jsonObject = new JsonObject();

        long availableSizeInBytes = new StatFs(diskPath.getPath()).getAvailableBytes();
        jsonObject.addProperty("available", availableSizeInBytes);

        long totalSizeInBytes = new StatFs(diskPath.getPath()).getTotalBytes();
        jsonObject.addProperty("total", totalSizeInBytes);

        return jsonObject;
    }

    public static JsonObject getDeviceDetails(Context context) {
        JsonObject jsonObject = new JsonObject();

        String os = "Android";
        jsonObject.addProperty("os", os);

        String model = Build.MODEL;
        jsonObject.addProperty("model", model);

        String manufacturer = Build.MANUFACTURER;
        jsonObject.addProperty("manufacturer", manufacturer);

        String brand = Build.BRAND;
        jsonObject.addProperty("brand", brand);

        String type = Build.TYPE;
        jsonObject.addProperty("type", type);

        String incremental = Build.VERSION.INCREMENTAL;
        jsonObject.addProperty("incremental", incremental);

        int version = Build.VERSION.SDK_INT;
        jsonObject.addProperty("version", version);

        String security_patch = Build.VERSION.SECURITY_PATCH;
        jsonObject.addProperty("security_patch", security_patch);

        return jsonObject;
    }

    public static JsonObject getCellTowersInfo(Context context) {
        JsonObject jsonObject = new JsonObject();

        // if permission is not granted, return empty object
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            jsonObject.addProperty("status", "permissions not granted");
            return jsonObject;
        }

        // all ok, continue collecting cell tower info
        jsonObject.addProperty("status", "ok");

        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        List<CellInfo> infos = tm.getAllCellInfo();

        JsonArray allCellTowers = new JsonArray();
        if (infos != null) {
            for (CellInfo info : infos) {
                JsonObject cellTowerInfoJson = getCellTowerInfo(context, info);
                allCellTowers.add(cellTowerInfoJson);
            }
        }
        jsonObject.add("all_cell_towers", allCellTowers);

        return jsonObject;
    }

    private static JsonObject getCellTowerInfo(Context context, CellInfo info) {

        JsonObject jsonObject = new JsonObject();

        boolean isRegistered = info.isRegistered();
        jsonObject.addProperty("is_registered", isRegistered);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            int cellConnectionStatus = info.getCellConnectionStatus();
            jsonObject.addProperty("cell_connection_status", cellConnectionStatus);
        }
        else {
            jsonObject.addProperty("cell_connection_status", -1);
        }

        JsonObject cellIdentityJson = new JsonObject();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            CellIdentity cellIdentity = info.getCellIdentity();

            String operatorAlphaShort = cellIdentity.getOperatorAlphaShort().toString();
            cellIdentityJson.addProperty("operator_alpha_short", operatorAlphaShort);

            String operatorAlphaLong = cellIdentity.getOperatorAlphaLong().toString();
            cellIdentityJson.addProperty("operator_alpha_long", operatorAlphaLong);
        }
        jsonObject.add("cell_identity", cellIdentityJson);

        JsonObject cellSignalStrengthJson = new JsonObject();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {

            CellSignalStrength cellSignalStrength = info.getCellSignalStrength();

            int asuLevel = cellSignalStrength.getAsuLevel();
            cellSignalStrengthJson.addProperty("asu_level", asuLevel);

            int dbm = cellSignalStrength.getDbm();
            cellSignalStrengthJson.addProperty("dbm", dbm);

            int level = cellSignalStrength.getLevel();
            cellSignalStrengthJson.addProperty("level", level);
        }
        jsonObject.add("cell_signal_strength", cellSignalStrengthJson);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            long timestampMillis = info.getTimestampMillis();
            jsonObject.addProperty("timestamp_millis", timestampMillis);
        }
        else {
            jsonObject.addProperty("timestamp_millis", -1);
        }

        long timestamp = info.getTimeStamp();
        jsonObject.addProperty("timestamp", timestamp);

        return jsonObject;
    }

    public static JsonObject getLocation(Context context) {
        JsonObject jsonObject = new JsonObject();

        // Check if feature is available
        PackageManager pm = context.getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_LOCATION_NETWORK)) {
            jsonObject.addProperty("status", "feature not available");
            return jsonObject;
        }

        // if permissions are not granted, return empty object
        if ((ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            jsonObject.addProperty("status", "permissions not granted");
            return jsonObject;
        }

        // all ok, continue collecting location
        jsonObject.addProperty("status", "ok");

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        long time = -1;//location.getTime();
        jsonObject.addProperty("time", time);

        double latitude = -1;//location.getLatitude();
        jsonObject.addProperty("latitude", latitude);

        double longitude = -1;//location.getLongitude();
        jsonObject.addProperty("longitude", longitude);

        if (location.hasAltitude()) {
            double altitude = -1;//location.getAltitude();
            jsonObject.addProperty("altitude", altitude);
        }
        else {
            jsonObject.addProperty("altitude", -1);
        }

        if (location.hasAccuracy()) {
            float accuracy = -1;//location.getAccuracy();
            jsonObject.addProperty("accuracy", accuracy);
        }
        else {
            jsonObject.addProperty("accuracy", -1);
        }

        if (location.hasVerticalAccuracy()) {
            float verticalAccuracy = -1;//location.getVerticalAccuracyMeters();
            jsonObject.addProperty("verticalAccuracy", verticalAccuracy);
        }
        else {
            jsonObject.addProperty("verticalAccuracy", -1);
        }

        return jsonObject;
    }

    public static JsonObject getUsageStatsDetails(Context context) {

        JsonObject jsonObject = new JsonObject();

        UsageStatsManager usageStatsManager = (UsageStatsManager) context.getSystemService(USAGE_STATS_SERVICE);
        if (usageStatsManager == null) {
            // not supported, return empty
            return jsonObject;
        }

        if (Build.VERSION.SDK_INT >= 28) {
            int appStandbyBucket = usageStatsManager.getAppStandbyBucket();
            jsonObject.addProperty("app_standby_bucket", appStandbyBucket);
        }
        else {
            jsonObject.addProperty("app_standby_bucket", -1);
        }

        // TODO: add more!

        return jsonObject;
    }
}
