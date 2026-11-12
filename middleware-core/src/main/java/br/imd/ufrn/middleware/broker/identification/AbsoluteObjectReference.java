package br.imd.ufrn.middleware.broker.identification;

/** Identification: Absolute Object Reference (protocolo + host + porta + Object Id). */
public final class AbsoluteObjectReference {

    private final String protocol;
    private final String host;
    private final int port;
    private final ObjectId objectId;

    public AbsoluteObjectReference(String protocol, String host, int port, ObjectId objectId) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.objectId = objectId;
    }

    public String protocol() {
        return protocol;
    }

    public String host() {
        return host;
    }

    public int port() {
        return port;
    }

    public ObjectId objectId() {
        return objectId;
    }

    @Override
    public String toString() {
        return protocol + "://" + host + ":" + port + "/" + objectId.value();
    }
}
