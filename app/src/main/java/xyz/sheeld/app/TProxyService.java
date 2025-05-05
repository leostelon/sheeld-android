package xyz.sheeld.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class TProxyService extends VpnService {
    private static native void TProxyStartService(String config_path, int fd);
    private static native void TProxyStopService();
    private static native long[] TProxyGetStats();
    private Timer timer;
    public long timeConsumed = 0;

    public static final String ACTION_CONNECT = "xyz.sheeld.app.CONNECT";
    public static final String ACTION_DISCONNECT = "xyz.sheeld.app.DISCONNECT";

    static {
        System.loadLibrary("hev-socks5-tunnel");
    }

    private ParcelFileDescriptor tunFd = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_DISCONNECT.equals(intent.getAction())) {
            stopService();
            return START_NOT_STICKY;
        }
        startService();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRevoke() {
        stopService();
        super.onRevoke();
    }

    public void startService() {
        if (tunFd != null)
            return;

        Preferences prefs = new Preferences(this);

        /* VPN */
        String session = new String();
        VpnService.Builder builder = new VpnService.Builder();
        builder.setBlocking(false);
        builder.setMtu(prefs.getTunnelMtu());
        if (prefs.getIpv4()) {
            String addr = prefs.getTunnelIpv4Address();
            int prefix = prefs.getTunnelIpv4Prefix();
            String dns = prefs.getDnsIpv4();
            builder.addAddress(addr, prefix);
            builder.addRoute("0.0.0.0", 0);
            if (!dns.isEmpty())
                builder.addDnsServer(dns);
            session += "IPv4";
        }
        if (prefs.getIpv6()) {
            String addr = prefs.getTunnelIpv6Address();
            int prefix = prefs.getTunnelIpv6Prefix();
            String dns = prefs.getDnsIpv6();
            builder.addAddress(addr, prefix);
            builder.addRoute("::", 0);
            if (!dns.isEmpty())
                builder.addDnsServer(dns);
            if (!session.isEmpty())
                session += " + ";
            session += "IPv6";
        }
        boolean disallowSelf = true;
        if (prefs.getGlobal()) {
            session += "/Global";
        } else {
            for (String appName : prefs.getApps()) {
                try {
                    builder.addAllowedApplication(appName);
                    disallowSelf = false;
                } catch (NameNotFoundException e) {
                }
            }
            session += "/per-App";
        }
        if (disallowSelf) {
            String selfName = getApplicationContext().getPackageName();
            try {
                builder.addDisallowedApplication(selfName);
            } catch (NameNotFoundException e) {
            }
        }
        builder.setSession(session);
        tunFd = builder.establish();
        if (tunFd == null) {
            stopSelf();
            return;
        }

        /* TProxy */
        File tproxy_file = new File(getCacheDir(), "tproxy.conf");
        try {
            tproxy_file.createNewFile();
            FileOutputStream fos = new FileOutputStream(tproxy_file, false);

            String tproxy_conf = "misc:\n" +
                    "  task-stack-size: " + prefs.getTaskStackSize() + "\n" +
                    "tunnel:\n" +
                    "  mtu: " + prefs.getTunnelMtu() + "\n";

            tproxy_conf += "socks5:\n" +
                    "  port: " + prefs.getSocksPort() + "\n" +
                    "  address: '" + prefs.getSocksAddress() + "'\n" +
                    "  udp: '" + (prefs.getUdpInTcp() ? "tcp" : "udp") + "'\n";

            if (!prefs.getSocksUsername().isEmpty() &&
                    !prefs.getSocksPassword().isEmpty()) {
                tproxy_conf += "  username: '" + prefs.getSocksUsername() + "'\n";
                tproxy_conf += "  password: '" + prefs.getSocksPassword() + "'\n";
            }

            fos.write(tproxy_conf.getBytes());
            fos.close();
        } catch (IOException e) {
            return;
        }
        TProxyStartService(tproxy_file.getAbsolutePath(), tunFd.getFd());
        prefs.setEnable(true);

        String channelName = "socks5";
        initNotificationChannel(channelName);
        createNotification(channelName);
        logStats();
    }

    public void stopService() {
        if (tunFd == null)
            return;

        stopForeground(true);

        /* TProxy */
        TProxyStopService();

        /* VPN */
        try {
            tunFd.close();
        } catch (IOException e) {
        }
        tunFd = null;
        timeConsumed = 0;
        DataManager.getInstance().setData(new long[]{0L, 0L, 0L, 0L});
        DataManager.getInstance().setTime(0);
        timer.cancel();
        SocketClient.getInstance().disconnect();
//        System.exit(0);
    }

    private void createNotification(String channelName) {
        Intent i = new Intent(this, TProxyService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, channelName);
        Notification notify = notification
                .setContentTitle(getString(R.string.app_name))
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setContentIntent(pi)
                .build();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notify);
        } else {
            startForeground(1, notify, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        }
    }

    // create NotificationChannel
    private void initNotificationChannel(String channelName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(channelName, name, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void logStats() {
        timeConsumed = 0;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long[] stats = TProxyGetStats();
                timeConsumed += 1;
                DataManager.getInstance().setData(stats);
                DataManager.getInstance().setTime(timeConsumed);
            }
        }, 0, 1000);
    }
}
