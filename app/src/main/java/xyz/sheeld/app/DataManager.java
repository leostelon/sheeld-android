package xyz.sheeld.app;

import java.util.HashSet;
import java.util.Set;

public class DataManager {
    private static DataManager instance;
    private final Set<DataUpdateListener> listeners = new HashSet<>();
    private long[] data = new long[4];
    private long timeConsumed = 0;
    private int latency = 0;

    private DataManager() {}

    public boolean isListenerActive() {
        return listeners.isEmpty();
    }

    public static synchronized DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public void setData(long[] newData, long newTime) {
        this.data = newData;
        this.timeConsumed = newTime;
        notifyListeners();
    }

    public void setLatency(int newLatency) {
        this.latency = newLatency;
        notifyListeners();
    }

    public void addListener(DataUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (DataUpdateListener listener : listeners) {
            listener.onDataUpdated(data, timeConsumed);
            listener.onLatencyUpdated(latency);
        }
    }
}