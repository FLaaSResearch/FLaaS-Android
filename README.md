# FLaaS-Android

This is the Android-based client component of our FLaaS system. It consists of the following components:
- **FLaaS (also known as FLaaS-Local):** a standalone service that needs to be installed on the device and provides the core FL functionality to the device in accordance to the requests
sent by the Server.
- **FLaaSLib:** a mobile app library integrated within each FLaaS-enabled third-party app willing
to participate in FL training processes.
- **Red / Green / Blue apps:** example toy apps that utilise FLaaSLib, crated to demonstrate  the simplicity and efficiency of a third-party app registering with our system.

For more information about our system, please read the following publications:
- [FLaaS: Enabling Practical Federated Learning on Mobile Environments](http://arxiv.org/abs/2206.10963)
- [FLaaS: Federated Learning as a Service](https://arxiv.org/abs/2011.09359)
- [Demo: FLaaS - Practical Federated Learning as a Service for Mobile Applications](https://dl.acm.org/doi/pdf/10.1145/3508396.3517074)


## Configuration

- Open the project using the latest version of [Android Studio](https://developer.android.com/studio).
- Configure the server host url at `org.sensingkit.flaas.network.APIService`.
- Configure your [Pushwoosh keys](https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/android-push-notifications/firebase-integration/integrate-pushwoosh-android-sdk) in the FLaaS app (`org.sensingkit.flaas`):
    * `com.pushwoosh.appid` in `AndroidManifest.xml`
    * `fcm_sender_id` in `strings.xml`
- Build and install the apps FLaaS, Red, Green and Blue into your devices.
