package br.imd.ufrn.middleware.broker.marshaller;

import br.imd.ufrn.middleware.broker.exceptions.InfrastructureException;

/** Marshaller: serializa / deserializa o payload. */
public interface Marshaller {

    String marshal(Object value) throws InfrastructureException;

    <T> T unmarshal(String payload, Class<T> type) throws InfrastructureException;
}
