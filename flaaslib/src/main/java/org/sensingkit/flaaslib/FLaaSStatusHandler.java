package org.sensingkit.flaaslib;

import org.sensingkit.flaaslib.enums.ErrorCode;

public interface FLaaSStatusHandler {

    void onStatusReceived(int requestID, boolean isSuccessful, ErrorCode errorCode);

}
