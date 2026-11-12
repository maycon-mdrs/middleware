package br.imd.ufrn.app.instance;

import br.imd.ufrn.middleware.broker.annotations.RemoteComponent;
import br.imd.ufrn.middleware.broker.annotations.RemoteMethod;
import br.imd.ufrn.middleware.broker.enums.HttpMethod;
import br.imd.ufrn.middleware.broker.enums.LifecyclePolicy;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Objeto remoto de uma zona (instância BR ou PT). */
@RemoteComponent(name = "zone-time", lifecycle = LifecyclePolicy.STATIC)
public class ZoneTimeService {

    private final String zone;
    private final ZoneId zoneId;

    public ZoneTimeService(String zone) {
        this.zone = zone.toLowerCase();
        this.zoneId = switch (this.zone) {
            case "br" -> ZoneId.of("America/Sao_Paulo");
            case "pt" -> ZoneId.of("Europe/Lisbon");
            default -> throw new IllegalArgumentException("Zona inválida: " + zone);
        };
    }

    @RemoteMethod(path = "/time", httpMethod = HttpMethod.GET)
    public String now() {
        return ZonedDateTime.now(zoneId).toString();
    }

    public String zone() {
        return zone;
    }
}
