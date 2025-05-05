package xyz.sheeld.app;

public class Util {
    public static String parseNodeAPIURL(String ip, int port) {
        String parsedIp = ip.startsWith("http://") || ip.startsWith("https://") ? ip : "http://" + ip;
        return parsedIp + ":" + port;
    }

    public static String removeIpSchemes(String ip) {
        return ip.replace("http://", "").replace("https", "");
    }
}
