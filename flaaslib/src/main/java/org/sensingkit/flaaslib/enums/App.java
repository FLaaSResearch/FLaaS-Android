package org.sensingkit.flaaslib.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

@SuppressWarnings("unused")
public enum App {

    FLAAS ("FLaaS", 100, "org.sensingkit.flaas"),
    RED ("Red", 0, "org.sensingkit.redapp"),
    GREEN ("Green", 1, "org.sensingkit.greenapp"),
    BLUE ("Blue", 2, "org.sensingkit.blueapp");

    private final String name;
    private final String packageName;
    private final int id;

    App(final @NonNull String name, final int id, final @NonNull String packageName) {
        this.name = name;
        this.id = id;
        this.packageName = packageName;
    }

    public @NonNull String getName() {
        return this.name;
    }

    public int getId() { return this.id; }

    public @NonNull String getPackageName() {
        return this.packageName;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getName();
    }

    public static App fromName(String name) throws IllegalArgumentException {
        return Arrays.stream(App.values())
                .filter(v -> v.name.equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown name: " + name));
    }

    public static App fromId(int id) throws IllegalArgumentException {
        return Arrays.stream(App.values())
                .filter(v -> v.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown id: " + id));
    }

    public static App fromPackageName(String packageName) throws IllegalArgumentException {
        return Arrays.stream(App.values())
                .filter(v -> v.packageName.equals(packageName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown packageName: " + packageName));
    }

    public static App[] rgbApps() {
        return new App[]{RED, GREEN, BLUE};
    }
}
