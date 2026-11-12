package br.imd.ufrn.middleware.extension.protocol.http;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Protocol Plug-In HTTP: ServerSocket + parse HTTP/1.0. */
public class HttpProtocolPlugin implements ProtocolPlugin {

    private final int port;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public HttpProtocolPlugin(int port) {
        this.port = port;
    }

    public int port() {
        return port;
    }

    @Override
    public String name() {
        return "HTTP";
    }

    @Override
    public void start(ServerRequestHandler handler) throws Exception {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[HTTP] ouvindo porta " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handleClient(socket, handler));
            }
        }
    }

    private void handleClient(Socket socket, ServerRequestHandler handler) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = in.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                write(s, Response.error(400, "{\"error\":\"Bad Request\"}"));
                return;
            }

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            if (tokenizer.countTokens() < 2) {
                write(s, Response.error(400, "{\"error\":\"Bad Request\"}"));
                return;
            }

            String methodToken = tokenizer.nextToken().toUpperCase();
            String path = tokenizer.nextToken();

            Map<String, String> headers = new HashMap<>();
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                }
            }

            int contentLength = 0;
            try {
                contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            } catch (NumberFormatException ignored) {
            }
            String body = "";
            if (contentLength > 0) {
                char[] buf = new char[contentLength];
                int read = in.read(buf, 0, contentLength);
                if (read > 0) {
                    body = new String(buf, 0, read);
                }
            }

            HttpMethod method;
            try {
                method = HttpMethod.valueOf(methodToken);
            } catch (IllegalArgumentException e) {
                write(s, Response.error(405, "{\"error\":\"Method Not Allowed\"}"));
                return;
            }

            Response response = handler.handle(new Request(method, path, body, headers));
            write(s, response);
        } catch (IOException e) {
            System.err.println("[HTTP] erro: " + e.getMessage());
        }
    }

    private static void write(Socket socket, Response response) throws IOException {
        byte[] bodyBytes = response.body().getBytes(StandardCharsets.UTF_8);
        String statusText = switch (response.status()) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Error";
        };
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
            out.writeBytes("HTTP/1.0 " + response.status() + " " + statusText + "\r\n");
            out.writeBytes("Content-Type: application/json; charset=utf-8\r\n");
            out.writeBytes("Content-Length: " + bodyBytes.length + "\r\n");
            out.writeBytes("Connection: close\r\n");
            out.writeBytes("\r\n");
            out.write(bodyBytes);
            out.flush();
        }
    }
}
