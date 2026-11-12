package br.imd.ufrn.middleware.extension.protocol.tcp;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Protocol Plug-In TCP: linha {@code TIME br|pt} → GET /time/{zone}.
 */
public class TcpProtocolPlugin implements ProtocolPlugin {

    private final int port;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public TcpProtocolPlugin(int port) {
        this.port = port;
    }

    @Override
    public String name() {
        return "TCP";
    }

    @Override
    public void start(ServerRequestHandler handler) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[TCP] ouvindo porta " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handleClient(socket, handler));
            }
        }
    }

    private void handleClient(Socket socket, ServerRequestHandler handler) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line = in.readLine();
            if (line == null || line.isBlank()) {
                out.println("ERROR empty");
                return;
            }

            String trimmed = line.trim();
            String zone;
            if (trimmed.toUpperCase().startsWith("TIME ")) {
                zone = trimmed.substring(5).trim().toLowerCase();
            } else {
                out.println("ERROR use: TIME br|pt");
                return;
            }

            Request request = new Request(HttpMethod.GET, "/time/" + zone, "", Map.of());
            Response response = handler.handle(request);
            if (response.status() == 200) {
                // body JSON string quoted → remove aspas externas se houver
                String body = response.body();
                if (body.startsWith("\"") && body.endsWith("\"") && body.length() >= 2) {
                    body = body.substring(1, body.length() - 1).replace("\\\"", "\"");
                }
                out.println("OK " + body);
            } else {
                out.println("ERROR " + response.body());
            }
        } catch (IOException e) {
            System.err.println("[TCP] erro: " + e.getMessage());
        }
    }
}
