package br.imd.ufrn.app.gateway;

import br.imd.ufrn.middleware.broker.annotations.PathParam;
import br.imd.ufrn.middleware.broker.annotations.RemoteComponent;
import br.imd.ufrn.middleware.broker.annotations.RemoteMethod;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.enums.LifecyclePolicy;
import br.imd.ufrn.middleware.broker.identification.AbsoluteObjectReference;
import br.imd.ufrn.middleware.broker.identification.Lookup;
import br.imd.ufrn.middleware.broker.identification.ObjectId;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Facade do gateway: escolhe instância saudável (LB) e encaminha HTTP ao backend.
 * Forward com {@link Socket} (como o TP1), não {@code HttpURLConnection} — keep-alive
 * do URLConnection sob carga gerava timeout ~2s e Error% espúrio após 20–30s.
 */
@RemoteComponent(name = "gateway-time", lifecycle = LifecyclePolicy.STATIC)
public class GatewayTimeService {

    private static final int CONNECT_TIMEOUT_MS = 2_000;
    private static final int READ_TIMEOUT_MS = 5_000;
    private static final int FORWARD_ATTEMPTS = 2;

    private final InstanceRegistry registry;
    private final Lookup lookup;

    public GatewayTimeService(InstanceRegistry registry, Lookup lookup) {
        this.registry = registry;
        this.lookup = lookup;
    }

    @RemoteMethod(path = "/time/{zone}", httpMethod = HttpMethod.GET)
    public String now(@PathParam("zone") String zone) {
        String z = zone.toLowerCase();
        if (!"br".equals(z) && !"pt".equals(z)) {
            throw new IllegalArgumentException("Zona inválida: " + zone + " (use br|pt)");
        }

        IllegalArgumentException last = null;
        for (int attempt = 0; attempt < FORWARD_ATTEMPTS; attempt++) {
            Optional<InstanceInfo> chosen = registry.nextHealthy(z);
            if (chosen.isEmpty()) {
                throw new IllegalArgumentException("Nenhuma instância saudável para zona: " + z);
            }

            InstanceInfo instance = chosen.get();
            lookup.bind("time-" + z, new AbsoluteObjectReference(
                    "http", instance.getHost(), instance.getPort(),
                    new ObjectId(instance.getInstanceId())));

            try {
                return forwardGet(instance);
            } catch (IllegalArgumentException e) {
                last = e;
            }
        }
        throw last != null ? last : new IllegalArgumentException("Falha ao encaminhar zona: " + z);
    }

    @RemoteMethod(path = "/registry", httpMethod = HttpMethod.GET)
    public String registry() {
        return registry.listAll().stream()
                .map(InstanceInfo::toString)
                .collect(Collectors.joining("\n"));
    }

    @RemoteMethod(path = "/lookup", httpMethod = HttpMethod.GET)
    public String lookupTable() {
        return lookup.asMap().entrySet().stream()
                .map(e -> e.getKey() + " -> " + e.getValue())
                .collect(Collectors.joining("\n"));
    }

    /** Mesma abordagem do HttpForwarder do TP1: Socket + Connection: close. */
    private static String forwardGet(InstanceInfo target) {
        String path = "/time";
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(target.getHost(), target.getPort()), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(READ_TIMEOUT_MS);

            String request = "GET " + path + " HTTP/1.0\r\n"
                    + "Host: " + target.getHost() + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";

            OutputStream rawOut = socket.getOutputStream();
            rawOut.write(request.getBytes(StandardCharsets.UTF_8));
            rawOut.flush();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = in.readLine();
            if (statusLine == null || statusLine.isBlank()) {
                throw new IllegalArgumentException("Resposta HTTP vazia de " + target.getInstanceId());
            }

            int contentLength = -1;
            String header;
            while ((header = in.readLine()) != null && !header.isEmpty()) {
                String lower = header.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
                }
            }

            String body;
            if (contentLength >= 0) {
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
            } else {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
                body = sb.toString();
            }

            body = body.trim();
            if (!statusLine.contains(" 200 ")) {
                throw new IllegalArgumentException(
                        "Backend " + target.getInstanceId() + " " + statusLine + ": " + body);
            }

            if (body.startsWith("\"") && body.endsWith("\"") && body.length() >= 2) {
                return body.substring(1, body.length() - 1).replace("\\\"", "\"");
            }
            return body;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Falha ao encaminhar para " + target.getHost() + ":" + target.getPort()
                            + " (" + target.getInstanceId() + "): " + e.getMessage(),
                    e);
        }
    }
}
