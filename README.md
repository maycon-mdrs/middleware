# middleware

Plataforma de middleware (2º trabalho DIM0614) + app de horário (gateway + instâncias BR/PT).

**Não entendeu o projeto?** Leia primeiro: [docs/como-funciona.md](docs/como-funciona.md) (explicação + Mermaid do zero).

## Módulos

| Módulo | Papel |
|--------|--------|
| `middleware-core` | JAR da plataforma (padrões de Remoting) |
| `time-app` | Gateway + instâncias que usam o JAR |

## Glossário

| Nome | Significado |
|------|-------------|
| `Invoker` | Chama o método Java com reflexão |
| `Marshaller` | JSON ↔ objeto |
| `ServerRequestHandler` | Recebe o pedido e encaminha |
| `RemoteObjectRegistry` | Mapa rota → método |
| `LifecycleManager` | Static, Per-Request, Pooling, … |
| `Lookup` / `ObjectId` / AOR | Identification |
| `ProtocolPlugin` | HTTP ou TCP |
| `LoggingInterceptor` | Log + latência |
| `@RemoteComponent` / `@RemoteMethod` | Modelo de componentes |

## Compilar

```powershell
mvn clean install
```

Classpath típico (Windows):

```powershell
$cp = "time-app\target\classes;middleware-core\target\classes;$env:USERPROFILE\.m2\repository\org\json\json\20250517\json-20250517.jar"
```

## Rodar (gateway sozinho)

O `GatewayApp` sobe HTTP/TCP/heartbeat, mas **`/time/*` exige pelo menos uma `InstanceApp` saudável**. Sem réplicas, a resposta é erro (HTTP 400) — necessário para a demo de kill total no JMeter.

```powershell
java -cp $cp br.imd.ufrn.app.GatewayApp
# GET /time/br sem instâncias → erro
# TCP: envie a linha "TIME br" na porta 9091 (mesmo comportamento)
```

## Rodar (arquitetura TP1 completa)

Na **raiz** do repo `middleware/` (depois de `mvn clean install`):

```powershell
# 1) gateway
java -cp $cp br.imd.ufrn.app.GatewayApp

# 2) ops — subir/derrubar instâncias (porta 8081)
java -cp $cp br.imd.ufrn.app.ops.InstanceManagerServer
```

Com o ops no ar:

```powershell
# subir
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"br\",\"id\":\"br-1\",\"port\":9101}"
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"br\",\"id\":\"br-2\",\"port\":9102}"
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"pt\",\"id\":\"pt-1\",\"port\":9201}"
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"pt\",\"id\":\"pt-2\",\"port\":9202}"

# listar
curl "http://127.0.0.1:8081/instances"

# derrubar (ex.: durante o JMeter)
curl -X DELETE "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"id\":\"br-1\"}"
```

Portas: HTTP `8080`, TCP `9091`, heartbeat `9000`, ops `8081`, instâncias `91xx/92xx`.

Úteis no gateway: `GET /registry`, `GET /lookup`.

## JMeter

Mesmo padrão do TP1: [jmeter/README.md](jmeter/README.md) · tutorial GUI: [jmeter/tutorial-gui.md](jmeter/tutorial-gui.md).

```powershell
cd jmeter
jmeter -n -t carga-http.jmx "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5" "-Jduration=60"
jmeter -n -t carga-tcp.jmx  "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5" "-Jduration=60"
jmeter -n -t carga-completa.jmx "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5" "-Jduration=30"
```

## Postman

Collection para ligar/desligar instâncias e checar o gateway: [postman/](postman/)  
Importar: `postman/middleware.postman_collection.json`.

AWS (EC2, igual espírito do TP1): [docs/ambiente-aws.md](docs/ambiente-aws.md).  
Boot automático na EC2: `bash scripts/install-systemd.sh` (ver docs).  
Knee / usable: [docs/aws-e-capacidade.md](docs/aws-e-capacidade.md).

## Fluxo

```
Cliente → ProtocolPlugin (HTTP|TCP)
       → ServerRequestHandler → Interceptor
       → Invoker + Lifecycle → método @RemoteMethod
       → Marshaller → Response
```

Gateway ainda: registry/heartbeat/LB → forward HTTP à instância.

## Critérios × código

| Critério | Onde |
|----------|------|
| Modelo de componentes | `@RemoteComponent`, `@RemoteMethod`, `@PathParam` |
| Basic Remoting | `handler`, `invoker`, `marshaller`, `exceptions` |
| Extension | `protocol/http`, `protocol/tcp`, `LoggingInterceptor` |
| Lifecycle | `broker/lifecycle/LifecycleManager` |
| Identification | `broker/identification` + `/lookup` |
| Asynchrony | adiado |

## Árvore (core)

```
Middleware.java
broker/annotations|enums|entities|exceptions|handler|invoker|marshaller|registry|lifecycle|identification
extension/interceptor|protocol/http|protocol/tcp
```
