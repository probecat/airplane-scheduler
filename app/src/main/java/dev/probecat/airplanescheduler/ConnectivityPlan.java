package dev.probecat.airplanescheduler;

final class ConnectivityPlan {
    private static final String[] WIFI_OFF = {"wifi", "set-wifi-enabled", "disabled"};
    private static final String[] AIRPLANE_ON = {"connectivity", "airplane-mode", "enable"};
    private static final String[] AIRPLANE_OFF = {"connectivity", "airplane-mode", "disable"};
    private static final String[] WIFI_ON = {"wifi", "set-wifi-enabled", "enabled"};

    static String[][] commands(boolean active, boolean changeWifi) {
        // Wi-Fi goes down before airplane mode starts, and comes up after it ends.
        if (active) {
            return changeWifi ? new String[][] {WIFI_OFF, AIRPLANE_ON} : new String[][] {AIRPLANE_ON};
        }
        return changeWifi ? new String[][] {AIRPLANE_OFF, WIFI_ON} : new String[][] {AIRPLANE_OFF};
    }
}
