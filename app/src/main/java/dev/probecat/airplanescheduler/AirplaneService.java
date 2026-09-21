package dev.probecat.airplanescheduler;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class AirplaneService extends IAirplaneService.Stub {
    public AirplaneService() {}

    @Override
    public int setEnabled(boolean enabled, boolean changeWifi) {
        int result = 0;
        for (String[] command : ConnectivityPlan.commands(enabled, changeWifi)) {
            if (run(command) != 0) {
                result = -1;
            }
        }
        return result;
    }

    private static int run(String... arguments) {
        try {
            String[] command = new String[arguments.length + 1];
            command[0] = "/system/bin/cmd";
            System.arraycopy(arguments, 0, command, 1, arguments.length);
            Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return -1;
            }
            return process.exitValue();
        } catch (IOException e) {
            return -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
