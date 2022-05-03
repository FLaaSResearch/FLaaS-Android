package org.sensingkit.flaaslib.broadcastdata;

import android.util.Log;

public class BroadcastData {

    @SuppressWarnings("unused")
    private static final String TAG = BroadcastData.class.getSimpleName();

    private final byte[] data;
    private int remainingPackets;

    private final boolean includesMetadata;
    private String metadata = null;

    public BroadcastData(int dataSize, int totalPackets, boolean includesMetadata) {
//        Log.d(TAG, "Init BroadcastData with dataSize: " + dataSize + " and totalPackets: " + totalPackets);

        data = new byte[dataSize];
        this.includesMetadata = includesMetadata;
        this.remainingPackets = totalPackets;
    }

    public boolean addPackets(byte[] data, int index) {
//        Log.d(TAG, "addPackets at index: " + index + " with size: " + data.length);

        System.arraycopy(data, 0, this.data, index, data.length);
        this.remainingPackets--;

        return isComplete();
    }

    public boolean addMetadata(String metadata) {
        this.metadata = metadata;

        return isComplete();
    }

    public boolean isComplete() {

        if (includesMetadata) {
            return this.remainingPackets == 0 && this.metadata != null;
        }
        else {
            return this.remainingPackets == 0;
        }
    }

    public byte[] getData() {
        return this.data;
    }

    public String getMetadata() {
        return this.metadata;
    }
}
