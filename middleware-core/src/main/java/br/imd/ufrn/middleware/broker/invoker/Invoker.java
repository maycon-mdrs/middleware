package br.imd.ufrn.middleware.broker.invoker;

import br.imd.ufrn.middleware.broker.entities.Request;
import br.imd.ufrn.middleware.broker.exceptions.ApplicationException;
import br.imd.ufrn.middleware.broker.exceptions.InfrastructureException;
import br.imd.ufrn.middleware.broker.registry.RemoteObjectRegistry;

/** Invoker: localiza o método do objeto remoto e executa. */
public class Invoker {

    private final RemoteObjectRegistry registry;

    public Invoker(RemoteObjectRegistry registry) {
        this.registry = registry;
    }

    public Object invoke(Request request) throws InfrastructureException, ApplicationException {
        // TODO:
        // 1. montar a rota (ex.: "GET /time/br")
        // 2. achar Method no registry
        // 3. resolver @PathParam / @QueryParam / @BodyParam / @HeaderParam
        // 4. method.invoke(instancia, args)
        throw new UnsupportedOperationException("Invoker ainda não implementado");
    }

    public RemoteObjectRegistry registry() {
        return registry;
    }
}
