# AWS e capacidade (knee / usable)

## Deploy sugerido (EC2)

1. Suba uma EC2 (Amazon Linux / Ubuntu) com JDK 21 e Maven.
2. Clone o repositório e rode `mvn clean install`.
3. Abra no security group as portas `8080` (HTTP), `9091` (TCP), `9000` (heartbeat) e as portas das instâncias (`9101`, `9102`, `9201`, `9202`).
4. No host:

```bash
# terminal 1 — gateway
java -cp "time-app/target/classes:middleware-core/target/classes:$HOME/.m2/repository/org/json/json/20250517/json-20250517.jar" \
  br.imd.ufrn.app.GatewayApp

# terminais 2–5 — instâncias
java -cp "time-app/target/classes:middleware-core/target/classes:$HOME/.m2/repository/org/json/json/20250517/json-20250517.jar" \
  br.imd.ufrn.app.instance.InstanceApp br br-1 9101
# ... br-2 9102, pt-1 9201, pt-2 9202
```

5. No JMeter (máquina local ou outra EC2), aponte `-JgatewayHost=<IP_PUBLICO>`.

## Knee capacity e Usable capacity

Repita a carga HTTP (`jmeter/carga-http.jmx`) aumentando `users` (ex.: 5, 8, 10, 15, 20, 30…):

| users | throughput (req/s) | latência p95 | erros % |
|-------|--------------------|--------------|---------|
| …     | …                  | …            | …       |

- **Knee capacity:** ponto em que o throughput para de crescer linearmente (ou a latência sobe rápido) — o “joelho” da curva.
- **Usable capacity:** tipicamente ~70–80% do knee (margem segura de operação), como no TP1.

Registre gráficos (throughput × users, latência × users) em `docs/figuras/` na apresentação.
