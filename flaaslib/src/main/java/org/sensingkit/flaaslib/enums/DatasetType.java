package org.sensingkit.flaaslib.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

@SuppressWarnings("unused")
public enum DatasetType {

    IID ("IID", "samples_iid.bin"),
    NON_IID ("NonIID", "samples_noniid.bin");

    private final String name;
    private final String filename;

    DatasetType(final @NonNull String name, final @NonNull String filename) {
        this.name = name;
        this.filename = filename;
    }

    public @NonNull String getName() {
        return this.name;
    }

    public @NonNull String getFilename() {
        return this.filename;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getName();
    }

    public static DatasetType fromName(String name) throws IllegalArgumentException {
        return Arrays.stream(DatasetType.values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
    }

    public static DatasetType fromFilename(String filename) throws IllegalArgumentException {
        return Arrays.stream(DatasetType.values())
                .filter(v -> v.filename.equals(filename))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown filename: " + filename));
    }
}
