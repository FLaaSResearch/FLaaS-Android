package org.sensingkit.flaaslib.broadcastdata;

import android.util.Log;

import org.sensingkit.flaaslib.enums.App;

import java.util.HashMap;
import java.util.Map;

public class BroadcastDataManager {

    @SuppressWarnings("unused")
    private static final String TAG = BroadcastDataManager.class.getSimpleName();

    private static BroadcastDataManager sBroadcastDataManager;

    // <AppName, requestID> to BroadcastData
    private final Map<String, Map<Integer, BroadcastData>> dataMap = new HashMap<>();

    public static BroadcastDataManager getInstance() {

        if (sBroadcastDataManager == null) {
            sBroadcastDataManager = new BroadcastDataManager();
        }
        return sBroadcastDataManager;
    }

    private BroadcastDataManager() {

        // Init for all apps
        for (App app : App.values()) {
            dataMap.put(app.getName(), new HashMap<>());
        }
    }

    public void initBroadcastData(App app, int requestID, int dataSize, int totalPackets, boolean includesMetadata) {

        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData != null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " already exists");
            return;
        }

        appMap.put(requestID, new BroadcastData(dataSize, totalPackets, includesMetadata));
    }

    public boolean packetsExist(App app, int requestID) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return false;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        return broadcastData != null;
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addPackets(App app, int requestID, byte[] data, int index) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return false;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData == null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " is null");
            return false;
        }

        return broadcastData.addPackets(data, index);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean addMetaData(App app, int requestID, String metadata) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return false;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData == null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " is null");
            return false;
        }

        return broadcastData.addMetadata(metadata);
    }

    public boolean isComplete(App app, int requestID) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return false;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData == null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " is null");
            return false;
        }

        return broadcastData.isComplete();
    }

    public byte[] getData(App app, int requestID) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return null;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData == null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " is null");
            return null;
        }

        return broadcastData.getData();
    }

    public String getMetadata(App app, int requestID) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return null;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData == null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " is null");
            return null;
        }

        return broadcastData.getMetadata();
    }

    public void clear(App app, int requestID) {
        Map<Integer, BroadcastData> appMap = dataMap.get(app.getName());
        if (appMap == null) {
            Log.e(TAG, "appMap for app " + app.getName() + " is null");
            return;
        }

        BroadcastData broadcastData = appMap.get(requestID);
        if (broadcastData == null) {
            Log.e(TAG, "broadcastData for app " + app.getName() + " and requestID " + requestID + " is null");
            return;
        }

        appMap.remove(requestID);
    }
}
