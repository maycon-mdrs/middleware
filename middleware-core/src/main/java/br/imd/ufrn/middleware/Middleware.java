package br.imd.ufrn.middleware;

import br.imd.ufrn.middleware.broker.annotations.RemoteComponent;
import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.broker.identification.AbsoluteObjectReference;
import br.imd.ufrn.middleware.broker.identification.Lookup;
import br.imd.ufrn.middleware.broker.identification.ObjectId;
import br.imd.ufrn.middleware.broker.invoker.Invoker;
import br.imd.ufrn.middleware.broker.marshaller.JsonMarshaller;
import br.imd.ufrn.middleware.broker.marshaller.Marshaller;
import br.imd.ufrn.middleware.broker.registry.RemoteObjectRegistry;
import br.imd.ufrn.middleware.extension.interceptor.InvocationInterceptor;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;
import br.imd.ufrn.middleware.extension.protocol.http.HttpProtocolPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Broker (bootstrap): monta as peças e sobe os Protocol Plug-Ins.
 *
 * <pre>
 * Middleware mw = new Middleware();
 * mw.register(new TimeService());
 * mw.protocol(new HttpProtocolPlugin(8080));
 * mw.protocol(new TcpProtocolPlugin(9091));
 * mw.start();
 * </pre>
 */
public class Middleware {

    private final RemoteObjectRegistry registry = new RemoteObjectRegistry();
    private final Lookup lookup = new Lookup();
    private final Marshaller marshaller = new JsonMarshaller();
    private final Invoker invoker = new Invoker(registry);
    private final ServerRequestHandler handler = new ServerRequestHandler(invoker, marshaller);

    private final List<ProtocolPlugin> protocols = new ArrayList<>();
    private final List<Object> pendingComponents = new ArrayList<>();
    private String lookupHost = "127.0.0.1";

    public Middleware() {
        invoker.setMarshaller(marshaller);
    }

    public Middleware lookupHost(String host) {
        this.lookupHost = host;
        return this;
    }

    public Middleware register(Object remoteComponent) {
        registry.register(remoteComponent);
        pendingComponents.add(remoteComponent);
        return this;
    }

    public RemoteObjectRegistry registry() {
        return registry;
    }

    public Lookup lookup() {
        return lookup;
    }

    public Middleware protocol(ProtocolPlugin plugin) {
        protocols.add(plugin);
        return this;
    }

    public Middleware interceptor(InvocationInterceptor interceptor) {
        handler.addInterceptor(interceptor);
        return this;
    }

    public void start() throws Exception {
        if (protocols.isEmpty()) {
            throw new IllegalStateException("Nenhum ProtocolPlugin registrado");
        }

        rebindLookup();

        for (ProtocolPlugin plugin : protocols) {
            Thread thread = new Thread(() -> {
                try {
                    plugin.start(handler);
                } catch (Exception e) {
                    System.err.println("[middleware] falha no " + plugin.name() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, "protocol-" + plugin.name());
            thread.start();
        }

        System.out.println("[middleware] iniciado com " + protocols.size() + " protocolo(s)");
        lookup.asMap().forEach((name, aor) ->
                System.out.println("[lookup] " + name + " -> " + aor));
    }

    private void rebindLookup() {
        int httpPort = protocols.stream()
                .filter(p -> p instanceof HttpProtocolPlugin)
                .map(p -> ((HttpProtocolPlugin) p).port())
                .findFirst()
                .orElse(8080);

        for (Object component : pendingComponents) {
            RemoteComponent meta = component.getClass().getAnnotation(RemoteComponent.class);
            if (meta != null) {
                lookup.bind(meta.name(),
                        new AbsoluteObjectReference("http", lookupHost, httpPort, new ObjectId(meta.name())));
            }
        }
    }
}
