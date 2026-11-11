# middleware

Plataforma de middleware (2º trabalho) + app de horário.

Pastas nomeadas pelo padrão de Remoting — abrir o Explorer já mostra onde cada peça mora.

## Módulos

| Módulo | Papel |
|--------|--------|
| `middleware-core` | JAR da plataforma |
| `time-app` | App que importa o JAR |

## Glossário

| Nome | Significado simples |
|------|---------------------|
| `Invoker` | Chama o método Java com reflexão |
| `Marshaller` | JSON ↔ objeto |
| `ServerRequestHandler` | Recebe o pedido e encaminha |
| `RemoteObjectRegistry` | Mapa rota → método |
| `ProtocolPlugin` | HTTP ou TCP na porta |
| `InvocationInterceptor` | Código antes/depois da invocação |
| `LifecyclePolicy` | Se reusa a instância ou cria outra |
| `@RemoteComponent` | Classe = objeto remoto |
| `@RemoteMethod` | Método = endpoint remoto |
| `@PathParam` / `@QueryParam` / `@BodyParam` / `@HeaderParam` | De onde vem cada argumento |

## Árvore

```
middleware/
  Middleware.java
  broker/
    annotations/   @RemoteComponent, @RemoteMethod, @PathParam, ...
    enums/         HttpMethod, LifecyclePolicy
    entities/      Request, Response
    exceptions/    RemotingException, ...
    handler/       ServerRequestHandler
    invoker/       Invoker
    marshaller/    Marshaller, JsonMarshaller
    registry/      RemoteObjectRegistry
  extension/
    interceptor/   InvocationInterceptor, InvocationContext
    protocol/      ProtocolPlugin
    protocol/http/ HttpProtocolPlugin
    protocol/tcp/  TcpProtocolPlugin
```

## Padrão → pasta

| Padrão | Pasta |
|--------|--------|
| Modelo de componentes | `broker/annotations`, `broker/enums` |
| Broker (bootstrap) | `Middleware.java` |
| Server Request Handler | `broker/handler` |
| Invoker | `broker/invoker` |
| Marshaller | `broker/marshaller` |
| Remote Object | `broker/registry` |
| Remoting Error | `broker/exceptions` |
| Invocation Interceptor / Context | `extension/interceptor` |
| Protocol Plug-In | `extension/protocol` (+ `http`, `tcp`) |

## Exemplo (app)

```java
@RemoteComponent(name = "time", lifecycle = STATIC)
public class TimeService {
  @RemoteMethod(path = "/time/{zone}", httpMethod = GET)
  public String now(@PathParam("zone") String zone) { ... }
}
```

## Compilar e rodar

```powershell
mvn clean install
java -cp "time-app\target\classes;middleware-core\target\classes;%USERPROFILE%\.m2\repository\org\json\json\20250517\json-20250517.jar" br.imd.ufrn.app.GatewayApp
```

Portas padrão: HTTP `8080`, TCP `9091` (esqueleto — ainda não aceitam conexão).

## Fluxo

```
Cliente → ProtocolPlugin → ServerRequestHandler
        → Interceptor(s) → Invoker → TimeService
        → Marshaller → Response
```

## Próxima fase

Core síncrono HTTP: registrar `@RemoteMethod`, Invoker real, `HttpProtocolPlugin` aceitando conexões.
