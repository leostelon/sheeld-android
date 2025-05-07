package xyz.sheeld.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.sheeld.app.api.types.Node;

public class DataManager {
    private static DataManager instance;
    private final Set<DataUpdateListener> listeners = new HashSet<>();
    private long[] data = new long[4];
    private long timeConsumed = 0;
    private int latency = 0;
    private List<Node> circuit = new ArrayList<>();
    private enum NOTIFY_TYPE {DATA, LATENCY, TIME, CIRCUIT,}

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

    public void setData(long[] newData) {
        this.data = newData;
        notifyListeners(NOTIFY_TYPE.DATA);
    }

    public void setLatency(int newLatency) {
        this.latency = newLatency;
        notifyListeners(NOTIFY_TYPE.LATENCY);
    }

    public void setTime(long t) {
        this.timeConsumed = t;
        notifyListeners(NOTIFY_TYPE.TIME);
    }

    public void setCircuit(List<Node> circuit) {
        this.circuit = circuit;
        notifyListeners(NOTIFY_TYPE.CIRCUIT);
    }

    public void addListener(DataUpdateListener listener) {
        listeners.add(listener);
    }

    public void removeListener(DataUpdateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(NOTIFY_TYPE type) {
        for (DataUpdateListener listener : listeners) {
            switch (type) {
                case DATA: listener.onDataUpdated(data);
                case TIME: listener.onTimeUpdated(timeConsumed);
                case LATENCY: listener.onLatencyUpdated(latency);
                case CIRCUIT: listener.onCircuitUpdated(circuit);
            }
        }
    }
}