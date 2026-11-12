package br.imd.ufrn.middleware.broker.lifecycle;

import br.imd.ufrn.middleware.broker.enums.LifecyclePolicy;
import br.imd.ufrn.middleware.broker.exceptions.InfrastructureException;
import br.imd.ufrn.middleware.broker.registry.RemoteObjectRegistry.Route;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lifecycle Management: obtém a instância conforme a política do @RemoteComponent.
 */
public class LifecycleManager {

    private final Map<String, Object> staticOrLazy = new ConcurrentHashMap<>();
    private final Map<String, Object> clientInstances = new ConcurrentHashMap<>();
    private final Map<String, Deque<Object>> pools = new ConcurrentHashMap<>();
    private final Map<String, Lease> leases = new ConcurrentHashMap<>();
    private final Map<String, Passivated> passivated = new ConcurrentHashMap<>();

    private final long leaseTtlMs;
    private final long passivationIdleMs;
    private final int poolSize;

    public LifecycleManager() {
        this(30_000L, 60_000L, 4);
    }

    public LifecycleManager(long leaseTtlMs, long passivationIdleMs, int poolSize) {
        this.leaseTtlMs = leaseTtlMs;
        this.passivationIdleMs = passivationIdleMs;
        this.poolSize = poolSize;
    }

    public Object getInstance(Route route, String clientId) throws InfrastructureException {
        String key = route.componentName();
        LifecyclePolicy policy = route.lifecycle();

        return switch (policy) {
            case STATIC -> route.instance();
            case PER_REQUEST -> newInstance(route);
            case CLIENT_DEPENDENT -> clientInstances.computeIfAbsent(
                    key + "::" + (clientId == null ? "anonymous" : clientId),
                    k -> safeNew(route));
            case LAZY_ACQUISITION -> staticOrLazy.computeIfAbsent(key, k -> route.instance());
            case POOLING -> borrowFromPool(route);
            case LEASING -> lease(route, clientId);
            case PASSIVATION -> fromPassivation(route);
        };
    }

    public void release(Route route, Object instance) {
        if (route.lifecycle() == LifecyclePolicy.POOLING && instance != null) {
            pools.computeIfAbsent(route.componentName(), k -> new ArrayDeque<>()).offerLast(instance);
        }
        if (route.lifecycle() == LifecyclePolicy.PASSIVATION && instance != null) {
            passivated.put(route.componentName(), new Passivated(instance, Instant.now()));
        }
    }

    public void renewLease(String componentName, String clientId) {
        String leaseKey = componentName + "::" + (clientId == null ? "anonymous" : clientId);
        Lease lease = leases.get(leaseKey);
        if (lease != null) {
            leases.put(leaseKey, new Lease(lease.instance(), Instant.now().plusMillis(leaseTtlMs)));
        }
    }

    private Object borrowFromPool(Route route) throws InfrastructureException {
        Deque<Object> pool = pools.computeIfAbsent(route.componentName(), k -> {
            Deque<Object> created = new ArrayDeque<>();
            created.add(route.instance());
            for (int i = 1; i < poolSize; i++) {
                created.add(safeNew(route));
            }
            return created;
        });
        Object instance = pool.pollFirst();
        if (instance == null) {
            return newInstance(route);
        }
        return instance;
    }

    private Object lease(Route route, String clientId) throws InfrastructureException {
        purgeExpiredLeases();
        String leaseKey = route.componentName() + "::" + (clientId == null ? "anonymous" : clientId);
        Lease existing = leases.get(leaseKey);
        if (existing != null && existing.expiresAt().isAfter(Instant.now())) {
            return existing.instance();
        }
        Object instance = route.instance();
        leases.put(leaseKey, new Lease(instance, Instant.now().plusMillis(leaseTtlMs)));
        return instance;
    }

    private Object fromPassivation(Route route) throws InfrastructureException {
        purgePassivated();
        Passivated p = passivated.get(route.componentName());
        if (p != null) {
            passivated.put(route.componentName(), new Passivated(p.instance(), Instant.now()));
            return p.instance();
        }
        Object instance = route.instance();
        passivated.put(route.componentName(), new Passivated(instance, Instant.now()));
        return instance;
    }

    private void purgeExpiredLeases() {
        Instant now = Instant.now();
        Iterator<Map.Entry<String, Lease>> it = leases.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().expiresAt().isBefore(now)) {
                it.remove();
            }
        }
    }

    private void purgePassivated() {
        Instant threshold = Instant.now().minusMillis(passivationIdleMs);
        Iterator<Map.Entry<String, Passivated>> it = passivated.entrySet().iterator();
        while (it.hasNext()) {
            if (it.next().getValue().lastAccess().isBefore(threshold)) {
                it.remove();
            }
        }
    }

    private Object newInstance(Route route) throws InfrastructureException {
        try {
            return route.method().getDeclaringClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new InfrastructureException("Falha ao criar instância de " + route.componentName(), e);
        }
    }

    private Object safeNew(Route route) {
        try {
            return newInstance(route);
        } catch (InfrastructureException e) {
            throw new IllegalStateException(e);
        }
    }

    private record Lease(Object instance, Instant expiresAt) {}

    private record Passivated(Object instance, Instant lastAccess) {}
}
