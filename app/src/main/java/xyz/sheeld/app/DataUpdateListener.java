package xyz.sheeld.app;

public interface DataUpdateListener {
    void onDataUpdated(long[] stats, long timeConsumed);
    void onLatencyUpdated(int latency);
}