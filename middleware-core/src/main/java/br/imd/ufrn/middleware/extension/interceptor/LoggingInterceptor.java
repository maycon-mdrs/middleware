package br.imd.ufrn.middleware.extension.interceptor;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.exceptions.RemotingException;

/** Interceptor simples: loga método/path e latência. */
public class LoggingInterceptor implements InvocationInterceptor {

    @Override
    public void before(Request request, InvocationContext context) throws RemotingException {
        context.put("startNanos", System.nanoTime());
        System.out.println("[interceptor] → " + request.method() + " " + request.path());
    }

    @Override
    public void after(Request request, Response response, InvocationContext context) throws RemotingException {
        Long start = context.get("startNanos");
        long elapsedMs = start == null ? -1 : (System.nanoTime() - start) / 1_000_000L;
        System.out.println("[interceptor] ← " + response.status() + " (" + elapsedMs + " ms)");
    }
}
