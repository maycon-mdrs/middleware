# middleware

Plataforma de middleware (2º trabalho DIM0614) + app de horário (gateway + instâncias BR/PT).

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

## Rodar (mínimo — só gateway com fallback local)

```powershell
java -cp $cp br.imd.ufrn.app.GatewayApp
curl http://127.0.0.1:8080/time/br
# TCP: envie a linha "TIME br" na porta 9091
```

## Rodar (arquitetura TP1 completa)

```powershell
# 1) gateway
java -cp $cp br.imd.ufrn.app.GatewayApp

# 2) instâncias (outros terminais)
java -cp $cp br.imd.ufrn.app.instance.InstanceApp br br-1 9101
java -cp $cp br.imd.ufrn.app.instance.InstanceApp br br-2 9102
java -cp $cp br.imd.ufrn.app.instance.InstanceApp pt pt-1 9201
java -cp $cp br.imd.ufrn.app.instance.InstanceApp pt pt-2 9202
```

Portas: HTTP `8080`, TCP `9091`, heartbeat `9000`, instâncias `91xx/92xx`.

Úteis: `GET /registry`, `GET /lookup`.

## JMeter

```powershell
jmeter -n -t jmeter\carga-http.jmx "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5"
jmeter -n -t jmeter\carga-tcp.jmx  "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5"
```

AWS / knee / usable: ver [docs/aws-e-capacidade.md](docs/aws-e-capacidade.md).

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
