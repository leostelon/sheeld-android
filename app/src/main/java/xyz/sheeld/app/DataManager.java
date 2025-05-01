package xyz.sheeld.app;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

public class DataManager {
    private static DataManager instance;
    private final Set<DataUpdateListener> listeners = new HashSet<>();
    private long[] data = new long[4];

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
        Log.d("length", listeners.size()+"");
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
            listener.onDataUpdated(data);
        }
    }
}