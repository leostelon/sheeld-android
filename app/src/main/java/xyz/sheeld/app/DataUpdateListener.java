package xyz.sheeld.app;

import java.util.List;

import xyz.sheeld.app.api.types.Node;

public interface DataUpdateListener {
    void onDataUpdated(long[] stats);
    void onLatencyUpdated(int latency);
    void onTimeUpdated(long timeConsumed);
    void onCircuitUpdated(List<Node> nodes);
}