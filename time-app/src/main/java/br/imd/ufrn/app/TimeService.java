package br.imd.ufrn.app;

import br.imd.ufrn.middleware.broker.annotations.PathParam;
import br.imd.ufrn.middleware.broker.annotations.RemoteComponent;
import br.imd.ufrn.middleware.broker.annotations.RemoteMethod;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.enums.LifecyclePolicy;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Objeto remoto de horário. */
@RemoteComponent(name = "time", lifecycle = LifecyclePolicy.STATIC)
public class TimeService {

    @RemoteMethod(path = "/time/{zone}", httpMethod = HttpMethod.GET)
    public String now(@PathParam("zone") String zone) {
        ZoneId zoneId = switch (zone.toLowerCase()) {
            case "br" -> ZoneId.of("America/Sao_Paulo");
            case "pt" -> ZoneId.of("Europe/Lisbon");
            default -> throw new IllegalArgumentException("Zona inválida: " + zone + " (use br|pt)");
        };
        return ZonedDateTime.now(zoneId).toString();
    }
}
