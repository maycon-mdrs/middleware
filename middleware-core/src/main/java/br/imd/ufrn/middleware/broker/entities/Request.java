package br.imd.ufrn.middleware.broker.entities;

import br.imd.ufrn.middleware.broker.enums.HttpMethod;

import java.util.Collections;
import java.util.Map;

/** Dados de invocação independentes do protocolo. */
public final class Request {

    private final HttpMethod method;
    private final String path;
    private final String body;
    private final Map<String, String> headers;

    public Request(HttpMethod method, String path, String body, Map<String, String> headers) {
        this.method = method;
        this.path = path;
        this.body = body == null ? "" : body;
        this.headers = headers == null ? Map.of() : Collections.unmodifiableMap(headers);
    }

    public HttpMethod method() {
        return method;
    }

    public String path() {
        return path;
    }

    public String body() {
        return body;
    }

    public Map<String, String> headers() {
        return headers;
    }
}
