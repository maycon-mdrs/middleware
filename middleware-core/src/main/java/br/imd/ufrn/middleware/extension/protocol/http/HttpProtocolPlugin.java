package br.imd.ufrn.middleware.extension.protocol.http;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Protocol Plug-In HTTP com suporte a keep-alive (HTTP/1.1).
 * Essencial quando o JMeter roda no PC contra a EC2: sem reutilizar TCP,
 * cada request paga o RTT Brasil↔AWS e a vazão despenca após alguns segundos.
 */
public class HttpProtocolPlugin implements ProtocolPlugin {

    private static final int IDLE_TIMEOUT_MS = 30_000;

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
        try (ServerSocket serverSocket = new ServerSocket(port, 512)) {
            System.out.println("[HTTP] ouvindo porta " + port + " (keep-alive)");
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(IDLE_TIMEOUT_MS);
                pool.execute(() -> handleClient(socket, handler));
            }
        }
    }

    private void handleClient(Socket socket, ServerRequestHandler handler) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = s.getOutputStream()) {

            while (true) {
                String headerLine;
                try {
                    headerLine = in.readLine();
                } catch (SocketTimeoutException idle) {
                    break;
                }
                if (headerLine == null) {
                    break;
                }
                if (headerLine.isBlank()) {
                    continue;
                }

                StringTokenizer tokenizer = new StringTokenizer(headerLine);
                if (tokenizer.countTokens() < 2) {
                    write(out, Response.error(400, "{\"error\":\"Bad Request\"}"), false);
                    break;
                }

                String methodToken = tokenizer.nextToken().toUpperCase(Locale.ROOT);
                String path = tokenizer.nextToken();
                String version = tokenizer.hasMoreTokens() ? tokenizer.nextToken() : "HTTP/1.0";

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
                    contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0").trim());
                } catch (NumberFormatException ignored) {
                }
                String body = "";
                if (contentLength > 0) {
                    char[] buf = new char[contentLength];
                    int read = 0;
                    while (read < contentLength) {
                        int n = in.read(buf, read, contentLength - read);
                        if (n < 0) {
                            break;
                        }
                        read += n;
                    }
                    body = new String(buf, 0, read);
                }

                boolean keepAlive = wantsKeepAlive(version, headers);

                HttpMethod method;
                try {
                    method = HttpMethod.valueOf(methodToken);
                } catch (IllegalArgumentException e) {
                    write(out, Response.error(405, "{\"error\":\"Method Not Allowed\"}"), false);
                    break;
                }

                Response response = handler.handle(new Request(method, path, body, headers));
                write(out, response, keepAlive);
                if (!keepAlive) {
                    break;
                }
            }
        } catch (IOException e) {
            // cliente fechou / reset — normal sob carga
        }
    }

    private static boolean wantsKeepAlive(String version, Map<String, String> headers) {
        String connection = headerIgnoreCase(headers, "Connection");
        if (connection != null && connection.equalsIgnoreCase("close")) {
            return false;
        }
        if (connection != null && connection.toLowerCase(Locale.ROOT).contains("keep-alive")) {
            return true;
        }
        // HTTP/1.1 default = keep-alive; HTTP/1.0 default = close
        return version != null && version.toUpperCase(Locale.ROOT).startsWith("HTTP/1.1");
    }

    private static String headerIgnoreCase(Map<String, String> headers, String name) {
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static void write(OutputStream out, Response response, boolean keepAlive) throws IOException {
        byte[] bodyBytes = response.body().getBytes(StandardCharsets.UTF_8);
        String statusText = switch (response.status()) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Error";
        };
        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(response.status()).append(' ').append(statusText).append("\r\n");
        sb.append("Content-Type: application/json; charset=utf-8\r\n");
        sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        if (keepAlive) {
            sb.append("Connection: keep-alive\r\n");
            sb.append("Keep-Alive: timeout=30\r\n");
        } else {
            sb.append("Connection: close\r\n");
        }
        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.US_ASCII));
        out.write(bodyBytes);
        out.flush();
    }
}
