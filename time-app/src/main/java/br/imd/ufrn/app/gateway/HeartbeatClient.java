package br.imd.ufrn.app.gateway;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/** Cliente REGISTER + HEARTBEAT periódico para o gateway. */
public class HeartbeatClient extends Thread {

    private final String gatewayHost;
    private final int gatewayPort;
    private final String serviceType;
    private final String advertiseHost;
    private final int listenPort;
    private final String instanceId;

    public HeartbeatClient(
            String gatewayHost,
            int gatewayPort,
            String serviceType,
            String advertiseHost,
            int listenPort,
            String instanceId) {
        super("heartbeat-" + instanceId);
        this.gatewayHost = gatewayHost;
        this.gatewayPort = gatewayPort;
        this.serviceType = serviceType;
        this.advertiseHost = advertiseHost;
        this.listenPort = listenPort;
        this.instanceId = instanceId;
        setDaemon(true);
    }

    @Override
    public void run() {
        send("REGISTER " + serviceType + " " + instanceId + " " + advertiseHost + " " + listenPort);
        while (!isInterrupted()) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                interrupt();
                break;
            }
            send("HEARTBEAT " + instanceId);
        }
    }

    private void send(String message) {
        try (Socket socket = new Socket(gatewayHost, gatewayPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            out.println(message);
            in.readLine();
        } catch (Exception e) {
            System.err.println("[heartbeat-client] " + instanceId + ": " + e.getMessage());
        }
    }
}
