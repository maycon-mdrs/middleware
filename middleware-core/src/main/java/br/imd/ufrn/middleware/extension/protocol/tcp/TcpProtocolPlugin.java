package br.imd.ufrn.middleware.extension.protocol.tcp;

import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;
import br.imd.ufrn.middleware.extension.protocol.ProtocolPlugin;

/** Protocol Plug-In TCP: ServerSocket + linha de texto. */
public class TcpProtocolPlugin implements ProtocolPlugin {

    private final int port;

    public TcpProtocolPlugin(int port) {
        this.port = port;
    }

    @Override
    public String name() {
        return "TCP";
    }

    @Override
    public void start(ServerRequestHandler handler) throws Exception {
        // TODO: ServerSocket + ler linha → Request → handler.handle → escrever Response
        System.out.println("[TCP] esqueleto na porta " + port + " (ainda não aceita conexões)");
    }
}
