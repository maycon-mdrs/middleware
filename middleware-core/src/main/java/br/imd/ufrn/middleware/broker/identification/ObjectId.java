package br.imd.ufrn.middleware.broker.identification;

import java.util.Objects;

/** Identification: Object Id — identificador lógico do objeto remoto. */
public final class ObjectId {

    private final String value;

    public ObjectId(String value) {
        this.value = Objects.requireNonNull(value, "objectId");
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectId other)) return false;
        return value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
