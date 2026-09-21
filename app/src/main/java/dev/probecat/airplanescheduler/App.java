package dev.probecat.airplanescheduler;

import android.app.Application;

import rikka.shizuku.Shizuku;

public final class App extends Application {
    // Reconcile a missed boundary whenever Shizuku starts or restarts later.
    private final Shizuku.OnBinderReceivedListener listener =
            () -> AirplaneController.applyCurrent(this, () -> {});

    @Override
    public void onCreate() {
        super.onCreate();
        Shizuku.addBinderReceivedListenerSticky(listener);
    }
}
