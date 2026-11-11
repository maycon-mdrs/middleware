package br.imd.ufrn.app;

import br.imd.ufrn.middleware.Middleware;
import br.imd.ufrn.middleware.extension.protocol.http.HttpProtocolPlugin;
import br.imd.ufrn.middleware.extension.protocol.tcp.TcpProtocolPlugin;

/** Entrada da aplicação: registra o TimeService e sobe HTTP + TCP. */
public final class GatewayApp {

    private GatewayApp() {}

    public static void main(String[] args) throws Exception {
        int httpPort = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        int tcpPort = args.length > 1 ? Integer.parseInt(args[1]) : 9091;

        Middleware mw = new Middleware();
        mw.register(new TimeService());
        mw.protocol(new HttpProtocolPlugin(httpPort));
        mw.protocol(new TcpProtocolPlugin(tcpPort));
        mw.start();

        System.out.println("[time-app] HTTP " + httpPort + " | TCP " + tcpPort);
        Thread.currentThread().join();
    }
}
