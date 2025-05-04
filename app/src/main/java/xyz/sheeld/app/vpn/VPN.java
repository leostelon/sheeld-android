package xyz.sheeld.app.vpn;

import android.content.Context;

import xyz.sheeld.app.Preferences;

public class VPN {
    private static VPN instance;
    private final Preferences prefs;

    private VPN(Context context) {
        prefs = new Preferences(context);
    }

    public static VPN getInstance(Context context){
        if(instance == null){
            instance = new VPN(context);
        }
        return instance;
    }

    public void setNewNetwork(String ip, int networkPort) {
        prefs.setSocksAddress(ip.replace("https://", "").replace("http://", ""));
        prefs.setSocksPort(networkPort);
    }
}
