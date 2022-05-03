package org.sensingkit.flaaslib.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

public enum TrainingMode {

    BASELINE ("BASELINE", "Baseline"),
    JOINT_MODELS ("JOINT_MODELS", "Joint Models"),
    JOINT_SAMPLES ("JOINT_SAMPLES", "Joint Samples");

    private final String value;
    private final String text;

    TrainingMode(final @NonNull String value, final @NonNull String text) {
        this.value = value;
        this.text = text;
    }

    public @NonNull String getValue() {
        return this.value;
    }

    public @NonNull String getText() {
        return this.text;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getText();
    }

    public static TrainingMode fromValue(String value) throws IllegalArgumentException {
        return Arrays.stream(TrainingMode.values())
                .filter(v -> v.value.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown value: " + value));
    }

    public static TrainingMode fromText(String text) throws IllegalArgumentException {
        return Arrays.stream(TrainingMode.values())
                .filter(v -> v.text.equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown filename: " + text));
    }
}
