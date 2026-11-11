package br.imd.ufrn.middleware.broker.exceptions;

/** Remoting Error: erro de distribuição ou de negócio. */
public class RemotingException extends Exception {

    public RemotingException(String message) {
        super(message);
    }

    public RemotingException(String message, Throwable cause) {
        super(message, cause);
    }
}
