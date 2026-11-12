package br.imd.ufrn.middleware.broker.registry;

import br.imd.ufrn.middleware.broker.annotations.RemoteComponent;
import br.imd.ufrn.middleware.broker.annotations.RemoteMethod;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.enums.LifecyclePolicy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Remote Object: guarda instâncias e rotas dos objetos remotos. */
public class RemoteObjectRegistry {

    public record Route(
            HttpMethod httpMethod,
            String pathPattern,
            Method method,
            Object instance,
            String componentName,
            LifecyclePolicy lifecycle
    ) {}

    private final Map<String, Object> instances = new ConcurrentHashMap<>();
    private final List<Route> routes = new ArrayList<>();

    public synchronized void register(Object component) {
        Class<?> type = component.getClass();
        RemoteComponent meta = type.getAnnotation(RemoteComponent.class);
        if (meta == null) {
            throw new IllegalArgumentException("Classe sem @RemoteComponent: " + type.getName());
        }

        instances.put(meta.name(), component);

        for (Method method : type.getDeclaredMethods()) {
            RemoteMethod rm = method.getAnnotation(RemoteMethod.class);
            if (rm == null) {
                continue;
            }
            method.setAccessible(true);
            routes.add(new Route(
                    rm.httpMethod(),
                    normalize(rm.path()),
                    method,
                    component,
                    meta.name(),
                    meta.lifecycle()
            ));
            System.out.println("[registry] " + rm.httpMethod() + " " + normalize(rm.path())
                    + " -> " + type.getSimpleName() + "." + method.getName());
        }
    }

    public Optional<Route> findRoute(HttpMethod method, String path) {
        String normalized = normalize(path);
        for (Route route : routes) {
            if (route.httpMethod() == method && pathMatches(route.pathPattern(), normalized)) {
                return Optional.of(route);
            }
        }
        return Optional.empty();
    }

    public Object getInstance(String componentName) {
        return instances.get(componentName);
    }

    public List<Route> routes() {
        return Collections.unmodifiableList(routes);
    }

    public Map<String, Object> instances() {
        return Collections.unmodifiableMap(instances);
    }

    public static boolean pathMatches(String pattern, String path) {
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        if (patternParts.length != pathParts.length) {
            return false;
        }
        for (int i = 0; i < patternParts.length; i++) {
            String pp = patternParts[i];
            if (pp.startsWith("{") && pp.endsWith("}")) {
                continue;
            }
            if (!pp.equals(pathParts[i])) {
                return false;
            }
        }
        return true;
    }

    public static String extractPathParam(String pattern, String path, String name) {
        String[] patternParts = pattern.split("/");
        String[] pathParts = path.split("/");
        for (int i = 0; i < patternParts.length; i++) {
            String pp = patternParts[i];
            if (pp.equals("{" + name + "}")) {
                return pathParts[i];
            }
        }
        return null;
    }

    private static String normalize(String path) {
        if (path == null || path.isBlank()) {
            return "/";
        }
        String p = path.trim();
        int q = p.indexOf('?');
        if (q >= 0) {
            p = p.substring(0, q);
        }
        if (!p.startsWith("/")) {
            p = "/" + p;
        }
        if (p.length() > 1 && p.endsWith("/")) {
            p = p.substring(0, p.length() - 1);
        }
        return p;
    }
}
