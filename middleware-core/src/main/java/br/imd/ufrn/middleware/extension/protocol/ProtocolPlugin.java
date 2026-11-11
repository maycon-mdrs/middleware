package br.imd.ufrn.middleware.extension.protocol;

import br.imd.ufrn.middleware.broker.handler.ServerRequestHandler;

/** Protocol Plug-In: transporte pluggable (HTTP, TCP, ...). */
public interface ProtocolPlugin {

    String name();

    void start(ServerRequestHandler handler) throws Exception;
}
