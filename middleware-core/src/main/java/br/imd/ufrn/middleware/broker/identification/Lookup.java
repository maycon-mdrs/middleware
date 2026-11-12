package br.imd.ufrn.middleware.broker.identification;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Identification: Lookup — nome lógico → Absolute Object Reference. */
public class Lookup {

    private final Map<String, AbsoluteObjectReference> table = new ConcurrentHashMap<>();

    public void bind(String name, AbsoluteObjectReference reference) {
        table.put(name, reference);
    }

    public AbsoluteObjectReference lookup(String name) {
        AbsoluteObjectReference ref = table.get(name);
        if (ref == null) {
            throw new IllegalArgumentException("Nome não encontrado no Lookup: " + name);
        }
        return ref;
    }

    public Optional<AbsoluteObjectReference> find(String name) {
        return Optional.ofNullable(table.get(name));
    }

    public void unbind(String name) {
        table.remove(name);
    }

    public Collection<AbsoluteObjectReference> all() {
        return table.values();
    }

    public Map<String, AbsoluteObjectReference> asMap() {
        return Map.copyOf(table);
    }
}
