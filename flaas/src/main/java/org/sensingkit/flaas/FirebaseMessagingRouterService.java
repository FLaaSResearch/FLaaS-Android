package org.sensingkit.flaas;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.pushwoosh.firebase.PushwooshFcmHelper;

public class FirebaseMessagingRouterService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        PushwooshFcmHelper.onTokenRefresh(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        if (PushwooshFcmHelper.isPushwooshMessage(remoteMessage)) {
            //this is a Pushwoosh push, SDK will handle it automatically
            PushwooshFcmHelper.onMessageReceived(this, remoteMessage);
        } else {
            //this is not a Pushwoosh push, you should handle it by yourself
            dispatchNonPushwooshMessage(remoteMessage);
        }
    }

    private void dispatchNonPushwooshMessage(RemoteMessage remoteMessage) {
        // Implement your push handling logics here
    }

}
