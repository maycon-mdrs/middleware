package br.imd.ufrn.app.gateway;

import br.imd.ufrn.app.TimeService;
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
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Facade do gateway: escolhe instância saudável (LB) e encaminha HTTP ao backend.
 * Sem instâncias, usa {@link TimeService} local (fallback).
 */
@RemoteComponent(name = "gateway-time", lifecycle = LifecyclePolicy.STATIC)
public class GatewayTimeService {

    private final InstanceRegistry registry;
    private final Lookup lookup;
    private final TimeService localFallback = new TimeService();

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

        Optional<InstanceInfo> chosen = registry.nextHealthy(z);
        if (chosen.isEmpty()) {
            return localFallback.now(z);
        }

        InstanceInfo instance = chosen.get();
        String aor = "http://" + instance.getHost() + ":" + instance.getPort() + "/time";
        lookup.bind("time-" + z, new AbsoluteObjectReference(
                "http", instance.getHost(), instance.getPort(),
                new ObjectId(instance.getInstanceId())));

        return forwardGet(aor);
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

    private static String forwardGet(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int status = conn.getResponseCode();
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            status >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String body = in.lines().collect(Collectors.joining());
                if (status >= 400) {
                    throw new IllegalArgumentException("Backend " + status + ": " + body);
                }
                if (body.startsWith("\"") && body.endsWith("\"") && body.length() >= 2) {
                    return body.substring(1, body.length() - 1).replace("\\\"", "\"");
                }
                return body;
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao encaminhar para " + url + ": " + e.getMessage(), e);
        }
    }
}
