package br.imd.ufrn.middleware.broker.handler;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.entities.Response;
import br.imd.ufrn.middleware.broker.exceptions.ApplicationException;
import br.imd.ufrn.middleware.broker.exceptions.InfrastructureException;
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

    public Response handle(Request request) {
        InvocationContext context = new InvocationContext();
        try {
            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.before(request, context);
            }

            Object result = invoker.invoke(request);
            String body = marshaller.marshal(result);
            Response response = Response.ok(body);

            for (InvocationInterceptor interceptor : interceptors) {
                interceptor.after(request, response, context);
            }
            return response;
        } catch (ApplicationException e) {
            return Response.error(400, jsonError(e.getMessage()));
        } catch (InfrastructureException e) {
            int status = e.getMessage() != null && e.getMessage().startsWith("Rota não encontrada") ? 404 : 500;
            return Response.error(status, jsonError(e.getMessage()));
        } catch (Exception e) {
            return Response.error(500, jsonError(e.getMessage()));
        }
    }

    private static String jsonError(String message) {
        String safe = message == null ? "erro" : message.replace("\"", "'");
        return "{\"error\":\"" + safe + "\"}";
    }
}
