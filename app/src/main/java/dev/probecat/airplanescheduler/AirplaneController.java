package dev.probecat.airplanescheduler;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import rikka.shizuku.Shizuku;

final class AirplaneController {
    static void applyCurrent(Context context, Runnable done) {
        Context app = context.getApplicationContext();
        boolean cleanup = Scheduler.cleanupPending(app);
        if (!Scheduler.isSaved(app) && !cleanup) {
            done.run();
            return;
        }
        boolean enabled = Scheduler.isSaved(app) && Scheduler.shouldEnableNow(app);
        boolean changeWifi = enabled ? Scheduler.disableWifi(app)
                : cleanup ? Scheduler.cleanupEnableWifi(app) : Scheduler.enableWifi(app);
        set(app, enabled, changeWifi, success -> {
            if (success && cleanup && !Scheduler.isSaved(app)) {
                Scheduler.cleanupFinished(app);
            }
            done.run();
        });
    }

    private static void set(Context context, boolean enabled, boolean changeWifi, Consumer<Boolean> done) {
        Context app = context.getApplicationContext();
        Handler main = new Handler(Looper.getMainLooper());
        AtomicBoolean completed = new AtomicBoolean();
        Consumer<Boolean> finish = success -> {
            if (completed.compareAndSet(false, true)) {
                done.accept(success);
            }
        };
        Runnable bind = () -> {
            try {
                if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                    finish.accept(false);
                    return;
                }
                Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(new ComponentName(app, AirplaneService.class))
                        .processNameSuffix("airplane")
                        .daemon(false);
                ServiceConnection connection = new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder binder) {
                        new Thread(() -> {
                            boolean success = false;
                            try {
                                success = IAirplaneService.Stub.asInterface(binder)
                                        .setEnabled(enabled, changeWifi) == 0;
                            } catch (Exception ignored) {
                            }
                            boolean result = success;
                            main.post(() -> finish.accept(result));
                            try {
                                Shizuku.unbindUserService(args, this, true);
                            } catch (RuntimeException ignored) {
                            }
                        }).start();
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        finish.accept(false);
                    }
                };
                Shizuku.bindUserService(args, connection);
            } catch (RuntimeException e) {
                finish.accept(false);
            }
        };
        if (Shizuku.pingBinder()) {
            bind.run();
        } else {
            Shizuku.OnBinderReceivedListener[] listener = new Shizuku.OnBinderReceivedListener[1];
            listener[0] = () -> {
                Shizuku.removeBinderReceivedListener(listener[0]);
                bind.run();
            };
            Shizuku.addBinderReceivedListener(listener[0]);
            main.postDelayed(() -> {
                Shizuku.removeBinderReceivedListener(listener[0]);
                finish.accept(false);
            }, 7000);
        }
    }
}
