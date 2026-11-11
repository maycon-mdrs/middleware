package br.imd.ufrn.middleware.broker.handler;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.exceptions.RemotingException;
import br.imd.ufrn.middleware.broker.invoker.Invoker;
import br.imd.ufrn.middleware.broker.marshaller.Marshaller;
import br.imd.ufrn.middleware.extension.interceptor.InvocationContext;
import br.imd.ufrn.middleware.extension.interceptor.InvocationInterceptor;

import java.util.ArrayList;
import java.util.List;

/** Server Request Handler: recebe a Request e despacha ao Invoker. */
public class ServerRequestHandler {

    private final Invoker invoker;
    private final Marshaller marshaller;
    private final List<InvocationInterceptor> interceptors = new ArrayList<>();

    public ServerRequestHandler(Invoker invoker, Marshaller marshaller) {
        this.invoker = invoker;
        this.marshaller = marshaller;
    }

    public void addInterceptor(InvocationInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    public Response handle(Request request) throws RemotingException {
        InvocationContext context = new InvocationContext();

        // 1. before
        for (InvocationInterceptor interceptor : interceptors) {
            interceptor.before(request, context);
        }

        // 2. invoca o objeto remoto
        Object result = invoker.invoke(request);

        // 3. serializa a resposta
        String body = marshaller.marshal(result);
        Response response = Response.ok(body);

        // 4. after
        for (InvocationInterceptor interceptor : interceptors) {
            interceptor.after(request, response, context);
        }

        return response;
    }
}
