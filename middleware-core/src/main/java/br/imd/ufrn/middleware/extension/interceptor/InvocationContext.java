package br.imd.ufrn.middleware.extension.interceptor;

import java.util.HashMap;
import java.util.Map;

/** Invocation Context: dados compartilhados ao longo da invocação. */
public class InvocationContext {

    private final Map<String, Object> values = new HashMap<>();

    public void put(String key, Object value) {
        values.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) values.get(key);
    }
}
