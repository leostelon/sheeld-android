package xyz.sheeld.app;

public interface DataUpdateListener {
    void onDataUpdated(long[] stats);
    void onLatencyUpdated(int latency);
    void onTimeUpdated(long timeConsumed);
}