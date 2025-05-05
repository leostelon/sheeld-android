package xyz.sheeld.app;

public class Util {
    public static String parseNodeAPIURL(String ip, int port) {
        String parsedIp = removeIpSchemes(ip);
        return parsedIp + ":" + port;
    }

    public static String removeIpSchemes(String ip) {
        return ip.startsWith("http://") || ip.startsWith("https://") ? ip : "http://" + ip;
    }
}
