package br.imd.ufrn.middleware.extension.protocol.http;

import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;

/** Protocol Plug-In HTTP: ServerSocket + parse HTTP/1.0. */
public class HttpProtocolPlugin implements ProtocolPlugin {

    private final int port;

    public HttpProtocolPlugin(int port) {
        this.port = port;
    }

    @Override
    public String name() {
        return "HTTP";
    }

    @Override
    public void start(ServerRequestHandler handler) throws Exception {
        // TODO: ServerSocket + ler request HTTP → Request → handler.handle → escrever Response
        System.out.println("[HTTP] esqueleto na porta " + port + " (ainda não aceita conexões)");
    }
}
