package br.imd.ufrn.app.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Registro de instâncias com round-robin (do TP1). */
public class InstanceRegistry {

    private final Map<String, InstanceInfo> instances = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobin = new ConcurrentHashMap<>();
    private final long heartbeatTimeoutMillis;

    public InstanceRegistry(long heartbeatTimeoutMillis) {
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
    }

    public void register(InstanceInfo instance) {
        boolean first = !instances.containsKey(instance.getInstanceId());
        instances.put(instance.getInstanceId(), instance);
        instance.refreshLastSeen();
        System.out.println((first ? "[NEW] " : "[REGISTER again] ") + instance);
    }

    public void heartbeat(String instanceId) {
        InstanceInfo instance = instances.get(instanceId);
        if (instance == null) {
            return;
        }
        instance.refreshLastSeen();
    }

    public void removeDeadInstances() {
        List<String> expired = new ArrayList<>();
        for (InstanceInfo instance : instances.values()) {
            if (!instance.isAlive(heartbeatTimeoutMillis)) {
                expired.add(instance.getInstanceId());
            }
        }
        for (String id : expired) {
            InstanceInfo removed = instances.remove(id);
            System.out.println("[TIMEOUT] removendo " + removed);
        }
    }

    public Optional<InstanceInfo> nextHealthy(String serviceType) {
        List<InstanceInfo> alive = listHealthy(serviceType);
        if (alive.isEmpty()) {
            return Optional.empty();
        }
        AtomicInteger counter = roundRobin.computeIfAbsent(serviceType, t -> new AtomicInteger(0));
        int position = Math.floorMod(counter.getAndIncrement(), alive.size());
        return Optional.of(alive.get(position));
    }

    public List<InstanceInfo> listHealthy(String serviceType) {
        List<InstanceInfo> result = new ArrayList<>();
        for (InstanceInfo instance : instances.values()) {
            if (instance.getServiceType().equalsIgnoreCase(serviceType)
                    && instance.isAlive(heartbeatTimeoutMillis)) {
                result.add(instance);
            }
        }
        return result;
    }

    public List<InstanceInfo> listAll() {
        return new ArrayList<>(instances.values());
    }
}
