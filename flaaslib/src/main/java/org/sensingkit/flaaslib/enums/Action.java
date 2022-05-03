package org.sensingkit.flaaslib.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

@SuppressWarnings("unused")
public enum Action {

    SEND_STATUS ("Send Status", "org.sensingkit.flaas.perform.SEND_STATUS"),
    REQUEST_SAMPLES ("Request Samples", "org.sensingkit.flaas.perform.REQUEST_SAMPLES"),
    SEND_SAMPLES ("Send Samples", "org.sensingkit.flaas.perform.SEND_SAMPLES"),
    REQUEST_TRAINING ("Request Training", "org.sensingkit.flaas.perform.REQUEST_TRAINING"),
    SEND_WEIGHTS ("Send Weights", "org.sensingkit.flaas.perform.SEND_WEIGHTS");
    private final String name;
    private final String action;

    Action(final @NonNull String name, final @NonNull String action) {
        this.name = name;
        this.action = action;
    }

    public @NonNull String getName() {
        return this.name;
    }

    public @NonNull String getAction() {
        return this.action;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getName();
    }

    public static Action fromName(String name) throws IllegalArgumentException {
        return Arrays.stream(Action.values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
    }

    public static Action fromAction(String action) throws IllegalArgumentException {
        return Arrays.stream(Action.values())
                .filter(v -> v.action.equals(action))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown action: " + action));
    }
}
