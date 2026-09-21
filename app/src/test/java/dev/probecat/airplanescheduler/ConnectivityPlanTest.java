package dev.probecat.airplanescheduler;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

public final class ConnectivityPlanTest {
    @Test
    public void startDisablesWifiBeforeEnablingAirplaneMode() {
        assertArrayEquals(new String[][] {
                {"wifi", "set-wifi-enabled", "disabled"},
                {"connectivity", "airplane-mode", "enable"}
        }, ConnectivityPlan.commands(true, true));
    }

    @Test
    public void endDisablesAirplaneModeBeforeEnablingWifi() {
        assertArrayEquals(new String[][] {
                {"connectivity", "airplane-mode", "disable"},
                {"wifi", "set-wifi-enabled", "enabled"}
        }, ConnectivityPlan.commands(false, true));
    }

    @Test
    public void wifiChangesCanBeDisabledIndependently() {
        assertArrayEquals(new String[][] {{"connectivity", "airplane-mode", "enable"}},
                ConnectivityPlan.commands(true, false));
        assertArrayEquals(new String[][] {{"connectivity", "airplane-mode", "disable"}},
                ConnectivityPlan.commands(false, false));
    }
}
