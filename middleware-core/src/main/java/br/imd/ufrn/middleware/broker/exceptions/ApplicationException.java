package br.imd.ufrn.middleware.broker.exceptions;

/** Remoting Error: falha na lógica de negócio do objeto remoto. */
public class ApplicationException extends RemotingException {

    public ApplicationException(String message) {
        super(message);
    }

    public ApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
