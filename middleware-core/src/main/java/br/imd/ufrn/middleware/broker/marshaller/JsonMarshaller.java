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
        try {
            return JSONObject.wrap(value).toString();
        } catch (Exception e) {
            throw new InfrastructureException("Falha ao serializar " + value.getClass().getName(), e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unmarshal(String payload, Class<T> type) throws InfrastructureException {
        if (payload == null || payload.isBlank() || "null".equals(payload)) {
            return null;
        }
        try {
            if (type == String.class) {
                if (payload.startsWith("\"") && payload.endsWith("\"")) {
                    return (T) new JSONObject("{\"v\":" + payload + "}").getString("v");
                }
                return (T) payload;
            }
            if (type == Integer.class || type == int.class) {
                return (T) Integer.valueOf(payload.trim());
            }
            if (type == Long.class || type == long.class) {
                return (T) Long.valueOf(payload.trim());
            }
            if (type == Boolean.class || type == boolean.class) {
                return (T) Boolean.valueOf(payload.trim());
            }
            if (type == Double.class || type == double.class) {
                return (T) Double.valueOf(payload.trim());
            }
            JSONObject json = new JSONObject(payload);
            return (T) json.toMap();
        } catch (Exception e) {
            throw new InfrastructureException("Falha ao deserializar para " + type.getName(), e);
        }
    }
}
