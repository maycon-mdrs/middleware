package br.imd.ufrn.app.gateway;

public class InstanceInfo {

    private final String instanceId;
    private final String serviceType;
    private final String host;
    private final int port;
    private volatile long lastSeenMillis;

    public InstanceInfo(String instanceId, String serviceType, String host, int port) {
        this.instanceId = instanceId;
        this.serviceType = serviceType;
        this.host = host;
        this.port = port;
        this.lastSeenMillis = System.currentTimeMillis();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void refreshLastSeen() {
        lastSeenMillis = System.currentTimeMillis();
    }

    public boolean isAlive(long timeoutMillis) {
        return System.currentTimeMillis() - lastSeenMillis <= timeoutMillis;
    }

    @Override
    public String toString() {
        return instanceId + " type=" + serviceType + " " + host + ":" + port
                + " lastSeen=" + lastSeenMillis;
    }
}
