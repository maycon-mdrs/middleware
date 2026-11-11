package br.imd.ufrn.middleware.broker.marshaller;

import br.imd.ufrn.middleware.broker.exceptions.InfrastructureException;
import org.json.JSONObject;

/** Marshaller JSON com org.json. */
public class JsonMarshaller implements Marshaller {

    @Override
    public String marshal(Object value) throws InfrastructureException {
        if (value == null) {
            return "null";
        }
        if (value instanceof String s) {
            return JSONObject.quote(s);
        }
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return JSONObject.wrap(value).toString();
    }

    @Override
    public <T> T unmarshal(String payload, Class<T> type) throws InfrastructureException {
        // TODO: deserializar JSON para o tipo pedido
        throw new UnsupportedOperationException("unmarshal ainda não implementado para " + type.getName());
    }
}
