package dev.probecat.airplanescheduler;

interface IAirplaneService {
    void destroy() = 16777114;
    int setEnabled(boolean enabled, boolean changeWifi) = 1;
}
