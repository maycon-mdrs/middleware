package br.imd.ufrn.app.gateway;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * TCP REGISTER / HEARTBEAT / LIST na porta 9000.
 * Formato: REGISTER type id host port | HEARTBEAT id | LIST
 */
public class HeartbeatServer implements Runnable {

    private final int port;
    private final InstanceRegistry registry;

    public HeartbeatServer(int port, InstanceRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void run() {
        var pool = Executors.newVirtualThreadPerTaskExecutor();
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[heartbeat] ouvindo " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = server.accept();
                pool.execute(() -> handle(socket));
            }
        } catch (Exception e) {
            System.err.println("[heartbeat] erro: " + e.getMessage());
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = in.readLine();
            if (line == null || line.isBlank()) {
                return;
            }
            String[] parts = line.trim().split("\\s+");
            String cmd = parts[0].toUpperCase();

            switch (cmd) {
                case "REGISTER" -> {
                    if (parts.length < 5) {
                        out.println("ERROR");
                        return;
                    }
                    registry.register(new InstanceInfo(parts[2], parts[1], parts[3], Integer.parseInt(parts[4])));
                    out.println("OK");
                }
                case "HEARTBEAT" -> {
                    if (parts.length < 2) {
                        out.println("ERROR");
                        return;
                    }
                    registry.heartbeat(parts[1]);
                    out.println("OK");
                }
                case "LIST" -> {
                    for (InstanceInfo info : registry.listAll()) {
                        out.println(info);
                    }
                    out.println("OK");
                }
                default -> out.println("ERROR unknown");
            }
        } catch (Exception e) {
            System.err.println("[heartbeat] " + e.getMessage());
        }
    }
}
