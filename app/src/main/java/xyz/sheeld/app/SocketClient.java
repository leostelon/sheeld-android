package xyz.sheeld.app;

import android.util.Log;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import java.net.URISyntaxException;

public class SocketClient {
    private static SocketClient socketClient;
    private Socket mSocket;

    public static synchronized SocketClient getInstance() {
        if (socketClient == null) {
            socketClient = new SocketClient();
        }
        return socketClient;
    }

    public void connectToServer(String ip, int port) {
        try {
            String serverUrl = "http://" + ip + ":" + port;
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;

            mSocket = IO.socket(serverUrl, options);

            mSocket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    Log.d("SocketIO", "Connected to " + serverUrl);
                }
            });

            mSocket.on("ping", args -> {
                Log.d("SocketIO", "Ping received, sending pong...");
                mSocket.emit("pong");
            });

            mSocket.on("latency", args -> {
                Log.d("SocketIO", "Latency: " + args[0] + " ms");
                DataManager.getInstance().setLatency(Integer.parseInt(String.valueOf(args[0])));
            });

            mSocket.on(Socket.EVENT_DISCONNECT, args -> Log.d("SocketIO", "Disconnected from server"));

            mSocket.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (mSocket != null) {
            mSocket.disconnect();
            mSocket.off();
        }
    }
}
