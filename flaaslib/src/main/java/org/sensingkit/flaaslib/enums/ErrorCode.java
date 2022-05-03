package org.sensingkit.flaaslib.enums;

import androidx.annotation.NonNull;

import java.util.Arrays;

public enum ErrorCode {

    NO_ERROR (-2, "Request was successful."),
    UNKNOWN_ERROR (-1, "An unknown error occurred."),

    USER_IS_NOT_LOGGED_IN(100, "User is not logged-in in the FLaaS app."),;

    private final int code;
    private final String error;

    ErrorCode(final int code, final @NonNull String error) {
        this.code = code;
        this.error = error;
    }

    public int getCode() { return this.code; }

    public @NonNull String getError() {
        return this.error;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getError();
    }

    public static ErrorCode fromCode(int code) throws IllegalArgumentException {
        return Arrays.stream(ErrorCode.values())
                .filter(v -> v.code == code)
                .findFirst()
                .orElse(UNKNOWN_ERROR);
    }
}
