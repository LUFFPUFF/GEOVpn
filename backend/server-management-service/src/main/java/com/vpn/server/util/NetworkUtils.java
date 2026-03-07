package com.vpn.server.util;

import org.springframework.stereotype.Component;
import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class NetworkUtils {
    public boolean isPortOpen(String ip, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
