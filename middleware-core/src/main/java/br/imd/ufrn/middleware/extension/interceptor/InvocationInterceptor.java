package br.imd.ufrn.middleware.extension.interceptor;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.exceptions.RemotingException;

/** Invocation Interceptor: lógica antes e depois da invocação. */
public interface InvocationInterceptor {

    default void before(Request request, InvocationContext context) throws RemotingException {
    }

    default void after(Request request, Response response, InvocationContext context) throws RemotingException {
    }
}
