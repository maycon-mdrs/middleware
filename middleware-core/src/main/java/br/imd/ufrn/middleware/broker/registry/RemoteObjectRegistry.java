package br.imd.ufrn.middleware.broker.registry;

import br.imd.ufrn.middleware.broker.annotations.RemoteComponent;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Remote Object: guarda instâncias e rotas dos objetos remotos. */
public class RemoteObjectRegistry {

    // nome do componente → instância
    private final Map<String, Object> instances = new HashMap<>();

    // "GET /time/{zone}" → Method
    private final Map<String, Method> methods = new HashMap<>();

    public void register(Object component) {
        Class<?> type = component.getClass();
        RemoteComponent meta = type.getAnnotation(RemoteComponent.class);
        if (meta == null) {
            throw new IllegalArgumentException("Classe sem @RemoteComponent: " + type.getName());
        }

        instances.put(meta.name(), component);

        // TODO: varrer métodos com @RemoteMethod e preencher o mapa methods
    }

    public Object getInstance(String componentName) {
        return instances.get(componentName);
    }

    public Method getMethod(String route) {
        return methods.get(route);
    }

    public Map<String, Method> methods() {
        return methods;
    }

    public Map<String, Object> instances() {
        return instances;
    }
}
