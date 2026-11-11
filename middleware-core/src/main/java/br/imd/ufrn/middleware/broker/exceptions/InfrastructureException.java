package br.imd.ufrn.middleware.broker.exceptions;

/** Remoting Error: falha de infraestrutura (socket, rota, marshalling). */
public class InfrastructureException extends RemotingException {

    public InfrastructureException(String message) {
        super(message);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
