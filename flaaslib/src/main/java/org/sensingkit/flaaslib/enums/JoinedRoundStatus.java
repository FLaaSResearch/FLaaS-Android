package org.sensingkit.flaaslib.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

public enum JoinedRoundStatus {

    JOIN_ROUND("join", 1),
    DOWNLOAD_MODEL("download_model", 2),
    TRAIN("train", 3),
    MERGE_MODELS("merge_models", 4),
    SUBMIT_RESULTS("submit_results", 5),
    COMPLETE("complete", 6);

    private final String name;
    private final int id;

    JoinedRoundStatus(final @NonNull String name, final int id) {
        this.name = name;
        this.id = id;
    }

    public @NonNull String getName() {
        return this.name;
    }

    public int getId() { return this.id; }

    @NonNull
    @Override
    public String toString() {
        return this.getName();
    }

    public static JoinedRoundStatus fromName(String name) throws IllegalArgumentException {
        return Arrays.stream(JoinedRoundStatus.values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
    }

    public static JoinedRoundStatus fromId(int id) throws IllegalArgumentException {
        return Arrays.stream(JoinedRoundStatus.values())
                .filter(v -> v.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown id: " + id));
    }
}
