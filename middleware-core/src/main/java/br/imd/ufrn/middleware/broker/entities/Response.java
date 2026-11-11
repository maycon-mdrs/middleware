package br.imd.ufrn.middleware.broker.entities;

/** Resposta já pronta para o Protocol Plug-In escrever na rede. */
public final class Response {

    private final int status;
    private final String body;

    public Response(int status, String body) {
        this.status = status;
        this.body = body == null ? "" : body;
    }

    public static Response ok(String body) {
        return new Response(200, body);
    }

    public static Response error(int status, String body) {
        return new Response(status, body);
    }

    public int status() {
        return status;
    }

    public String body() {
        return body;
    }
}
