package br.imd.ufrn.middleware;

import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.broker.invoker.Invoker;
import br.imd.ufrn.middleware.broker.marshaller.JsonMarshaller;
import br.imd.ufrn.middleware.broker.marshaller.Marshaller;
import br.imd.ufrn.middleware.broker.registry.RemoteObjectRegistry;
import br.imd.ufrn.middleware.extension.interceptor.InvocationInterceptor;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;

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
    private final Invoker invoker = new Invoker(registry);
    private final Marshaller marshaller = new JsonMarshaller();
    private final ServerRequestHandler handler = new ServerRequestHandler(invoker, marshaller);

    private final List<ProtocolPlugin> protocols = new ArrayList<>();

    public Middleware register(Object remoteComponent) {
        registry.register(remoteComponent);
        return this;
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
    }
}
