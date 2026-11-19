package br.imd.ufrn.middleware.extension.interceptor;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.exceptions.RemotingException;

/**
 * Interceptor simples: loga método/path e latência.
 * Em carga (JMeter), log a cada request satura I/O no t3.micro — por padrão só loga erros
 * ou 1 a cada {@code LOG_EVERY} sucessos. {@code -Dmw.log.all=true} volta o log completo.
 */
public class LoggingInterceptor implements InvocationInterceptor {

    private static final boolean LOG_ALL = Boolean.parseBoolean(System.getProperty("mw.log.all", "false"));
    private static final int LOG_EVERY = Integer.getInteger("mw.log.every", 50);
    private static final java.util.concurrent.atomic.AtomicLong SUCCESS = new java.util.concurrent.atomic.AtomicLong();

    @Override
    public void before(Request request, InvocationContext context) throws RemotingException {
        context.put("startNanos", System.nanoTime());
        if (LOG_ALL) {
            System.out.println("[interceptor] → " + request.method() + " " + request.path());
        }
    }

    @Override
    public void after(Request request, Response response, InvocationContext context) throws RemotingException {
        Long start = context.get("startNanos");
        long elapsedMs = start == null ? -1 : (System.nanoTime() - start) / 1_000_000L;
        boolean error = response.status() >= 400;
        boolean sample = !error && LOG_EVERY > 0 && SUCCESS.incrementAndGet() % LOG_EVERY == 0;
        if (LOG_ALL || error || sample) {
            System.out.println("[interceptor] ← " + response.status() + " (" + elapsedMs + " ms)"
                    + " " + request.method() + " " + request.path());
        }
    }
}
