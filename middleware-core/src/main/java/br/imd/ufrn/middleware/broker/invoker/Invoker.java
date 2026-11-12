package br.imd.ufrn.middleware.broker.invoker;

import br.imd.ufrn.middleware.broker.annotations.BodyParam;
import br.imd.ufrn.middleware.broker.annotations.HeaderParam;
import br.imd.ufrn.middleware.broker.annotations.PathParam;
import br.imd.ufrn.middleware.broker.annotations.QueryParam;
import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.exceptions.ApplicationException;
import br.imd.ufrn.middleware.broker.exceptions.InfrastructureException;
import br.imd.ufrn.middleware.broker.lifecycle.LifecycleManager;
import br.imd.ufrn.middleware.broker.marshaller.Marshaller;
import br.imd.ufrn.middleware.broker.registry.RemoteObjectRegistry;
import br.imd.ufrn.middleware.broker.registry.RemoteObjectRegistry.Route;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

/** Invoker: localiza o método do objeto remoto e executa. */
public class Invoker {

    private final RemoteObjectRegistry registry;
    private final LifecycleManager lifecycle = new LifecycleManager();
    private Marshaller marshaller;

    public Invoker(RemoteObjectRegistry registry) {
        this.registry = registry;
    }

    public void setMarshaller(Marshaller marshaller) {
        this.marshaller = marshaller;
    }

    public Object invoke(Request request) throws InfrastructureException, ApplicationException {
        Route route = registry.findRoute(request.method(), pathOnly(request.path()))
                .orElseThrow(() -> new InfrastructureException(
                        "Rota não encontrada: " + request.method() + " " + request.path()));

        String clientId = headerIgnoreCase(request.headers(), "X-Client-Id");
        Object instance = lifecycle.getInstance(route, clientId);
        Object[] args = resolveArgs(route, request);

        try {
            Object result = route.method().invoke(instance, args);
            lifecycle.release(route, instance);
            return result;
        } catch (InvocationTargetException e) {
            lifecycle.release(route, instance);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof IllegalArgumentException) {
                throw new ApplicationException(cause.getMessage(), cause);
            }
            throw new ApplicationException("Erro na lógica de negócio: " + cause.getMessage(), cause);
        } catch (IllegalAccessException e) {
            lifecycle.release(route, instance);
            throw new InfrastructureException("Não foi possível invocar o método", e);
        }
    }

    private Object[] resolveArgs(Route route, Request request) throws InfrastructureException, ApplicationException {
        Method method = route.method();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];
        String path = pathOnly(request.path());
        Map<String, String> query = parseQuery(request.path());

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            PathParam pathParam = parameter.getAnnotation(PathParam.class);
            if (pathParam != null) {
                args[i] = convert(
                        RemoteObjectRegistry.extractPathParam(route.pathPattern(), path, pathParam.value()),
                        parameter.getType());
                continue;
            }
            QueryParam queryParam = parameter.getAnnotation(QueryParam.class);
            if (queryParam != null) {
                args[i] = convert(query.get(queryParam.value()), parameter.getType());
                continue;
            }
            HeaderParam headerParam = parameter.getAnnotation(HeaderParam.class);
            if (headerParam != null) {
                args[i] = convert(headerIgnoreCase(request.headers(), headerParam.value()), parameter.getType());
                continue;
            }
            BodyParam bodyParam = parameter.getAnnotation(BodyParam.class);
            if (bodyParam != null) {
                if (parameter.getType() == String.class) {
                    args[i] = request.body();
                } else if (marshaller != null) {
                    args[i] = marshaller.unmarshal(request.body(), parameter.getType());
                } else {
                    throw new InfrastructureException("Marshaller não configurado para @BodyParam");
                }
                continue;
            }
            throw new InfrastructureException(
                    "Parâmetro sem anotação em " + method.getName() + ": " + parameter.getName());
        }
        return args;
    }

    private static String headerIgnoreCase(Map<String, String> headers, String name) {
        if (headers == null || name == null) {
            return null;
        }
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static Object convert(String value, Class<?> type) throws ApplicationException {
        if (value == null) {
            return null;
        }
        if (type == String.class) {
            return value;
        }
        try {
            if (type == int.class || type == Integer.class) {
                return Integer.parseInt(value);
            }
            if (type == long.class || type == Long.class) {
                return Long.parseLong(value);
            }
            if (type == boolean.class || type == Boolean.class) {
                return Boolean.parseBoolean(value);
            }
            if (type == double.class || type == Double.class) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            throw new ApplicationException("Valor inválido para " + type.getSimpleName() + ": " + value, e);
        }
        throw new ApplicationException("Tipo de parâmetro não suportado: " + type.getName());
    }

    private static String pathOnly(String pathAndQuery) {
        if (pathAndQuery == null) {
            return "/";
        }
        int q = pathAndQuery.indexOf('?');
        return q >= 0 ? pathAndQuery.substring(0, q) : pathAndQuery;
    }

    private static Map<String, String> parseQuery(String pathAndQuery) {
        Map<String, String> map = new HashMap<>();
        if (pathAndQuery == null) {
            return map;
        }
        int q = pathAndQuery.indexOf('?');
        if (q < 0 || q == pathAndQuery.length() - 1) {
            return map;
        }
        for (String pair : pathAndQuery.substring(q + 1).split("&")) {
            String[] kv = pair.split("=", 2);
            map.put(kv[0], kv.length > 1 ? kv[1] : "");
        }
        return map;
    }

    public RemoteObjectRegistry registry() {
        return registry;
    }

    public LifecycleManager lifecycle() {
        return lifecycle;
    }
}
