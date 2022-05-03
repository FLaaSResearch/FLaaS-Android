package org.sensingkit.flaaslib.utils;

public class PerformanceCheckpoint {

    // used for getting memory consumption
    private final Runtime runtime = Runtime.getRuntime();

    // Duration properties
    private long startTime;
    private long endTime;

    // Memory properties
    private long startMemory;
    private long endMemory;

    // start tracking
    public void start() {
        this.startTime = System.nanoTime();
        this.startMemory = runtime.freeMemory();
    }

    // stop tracking
    public void end() {
        this.endTime = System.nanoTime();
        this.endMemory = runtime.freeMemory();
    }

    // get duration
    public float getDuration() {
        return (float)((this.endTime - this.startTime) / 1e9);
    }

    // get memory delta
    public long getMemory() {
        return this.startMemory - this.endMemory;
    }
}
