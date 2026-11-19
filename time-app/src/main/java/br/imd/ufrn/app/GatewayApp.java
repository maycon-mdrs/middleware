package br.imd.ufrn.app;

import br.imd.ufrn.app.gateway.GatewayTimeService;
import br.imd.ufrn.app.gateway.HeartbeatServer;
import br.imd.ufrn.app.gateway.InstanceRegistry;
import br.imd.ufrn.middleware.Middleware;
import br.imd.ufrn.middleware.extension.interceptor.LoggingInterceptor;
import br.imd.ufrn.middleware.extension.protocol.http.HttpProtocolPlugin;
import br.imd.ufrn.middleware.extension.protocol.tcp.TcpProtocolPlugin;

/**
 * Gateway: middleware HTTP+TCP + registry/heartbeat do TP1.
 *
 * Uso: GatewayApp [httpPort] [tcpPort] [heartbeatPort]
 */
public final class GatewayApp {

    private GatewayApp() {}

    public static void main(String[] args) throws Exception {
        int httpPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int tcpPort = args.length > 1 ? Integer.parseInt(args[1]) : 9091;
        int heartbeatPort = args.length > 2 ? Integer.parseInt(args[2]) : 9000;

        // 20s: no t3.micro sob JMeter, 6s (TP1) tirava réplicas vivas e gerava Error% espúrio.
        InstanceRegistry registry = new InstanceRegistry(20_000);
        Thread cleaner = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(2000);
                    registry.removeDeadInstances();
                } catch (InterruptedException e) {
                    interruptQuietly();
                    break;
                }
            }
        }, "registry-cleaner");
        cleaner.setDaemon(true);
        cleaner.start();

        Thread heartbeat = new Thread(new HeartbeatServer(heartbeatPort, registry), "heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();

        Middleware mw = new Middleware();
        mw.register(new GatewayTimeService(registry, mw.lookup()));
        // Log amostrado só se pedir: -Dmw.gateway.log=true (evita contenção no System.out sob JMeter)
        if (Boolean.parseBoolean(System.getProperty("mw.gateway.log", "false"))) {
            mw.interceptor(new LoggingInterceptor());
        }
        mw.protocol(new HttpProtocolPlugin(httpPort));
        mw.protocol(new TcpProtocolPlugin(tcpPort));
        mw.start();

        System.out.println("[gateway] HTTP " + httpPort + " | TCP " + tcpPort + " | HEARTBEAT " + heartbeatPort);
        System.out.println("[gateway] curl http://127.0.0.1:" + httpPort + "/time/br");
        System.out.println("[gateway] ops (outro terminal): InstanceManagerServer 8081");
        System.out.println("[gateway]   POST/DELETE/GET http://127.0.0.1:8081/instances");
        Thread.currentThread().join();
    }

    private static void interruptQuietly() {
        Thread.currentThread().interrupt();
    }
}
