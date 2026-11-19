# Como o TP2 funciona (do zero)

Este arquivo existe para uma coisa só: **entender o projeto sem pirar**.

Leia na ordem. Os diagramas abrem no preview do Markdown do Cursor/VS Code / GitHub.

---

## 1. Em uma frase

Você criou uma **biblioteca** (`middleware-core`) que transforma método Java anotado em serviço na rede.

A **aplicação** (`time-app`) usa essa biblioteca — é a mesma ideia do TP1 (horário BR/PT + gateway), só que a rede não é mais escrita na mão na classe de negócio.

**JMeter não fala com o projeto `api-gateway`.** Fala com o `GatewayApp` deste repo.

---

## 2. TP1 vs TP2 (a confusão mais comum)

```mermaid
flowchart LR
  subgraph tp1 [Trabalho 1 - api-gateway]
    C1[Cliente / JMeter] --> G1[Gateway com ServerSocket]
    G1 --> I1[Instancia BR/PT com ServerSocket]
  end

  subgraph tp2 [Trabalho 2 - middleware]
    C2[Cliente / JMeter] --> G2[GatewayApp]
    G2 --> MW[middleware-core JAR]
    G2 --> I2[InstanceApp]
    I2 --> MW
  end
```

| | TP1 `api-gateway` | TP2 `middleware` |
|--|-------------------|------------------|
| Entrega deste trabalho | Não (já foi) | **Sim** |
| O que o JMeter chama | Processo do TP1 | **`GatewayApp` do TP2** |
| Como a rede funciona | Sockets na mão em tudo | Middleware (anotações + Invoker + plugins) |
| Relação entre eles | — | TP2 **refaz a mesma aplicação**, não chama o TP1 |

O PDF manda: *mesma aplicação da 1ª unidade, mas classes de negócio com anotações, e cada componente importa o JAR*. É isso que o `time-app` faz.

---

## 3. Dois projetos no IDE, um processo na hora de rodar

Isso é a figura do PDF.

```mermaid
flowchart TB
  JMeter[JMeter] -->|"HTTP :8080"| Processo

  subgraph Processo ["Mesmo processo Java - GatewayApp"]
    App[time-app: GatewayTimeService etc]
    Jar[middleware-core JAR: Invoker, HTTP plugin...]
    App -->|"usa o JAR"| Jar
  end
```

- No **IDE**: `middleware-core` e `time-app` são módulos separados.
- No **Maven**: `time-app` depende do JAR do `middleware-core`.
- No **SO**: `java ... GatewayApp` = **um** processo com as duas coisas no classpath.

Instâncias (`InstanceApp`) são **outros processos** (como no TP1). A figura do PDF mostra o caso mínimo; a arquitetura completa parece o TP1.

---

## 4. Para que serve o middleware?

Sem middleware (TP1):

```text
pedido HTTP → você parseia → if /time/br → devolve horário
```

Com middleware (TP2):

```text
você escreve só:

  @RemoteMethod(path = "/time/{zone}")
  public String now(String zone) { ... }

o middleware: escuta porta, acha o método, chama, responde
```

É o “motor” por trás de Spring/gRPC, só que feito por você para a disciplina.

---

## 5. Mapa mental das pastas

```mermaid
flowchart TB
  subgraph core [middleware-core = a plataforma]
    Mid[Middleware.java - liga tudo]
    Ann[annotations - RemoteComponent RemoteMethod PathParam]
    Reg[registry - tabela rota para metodo]
    Inv[invoker - method.invoke]
    Hand[handler - orquestra o pedido]
    Mar[marshaller - JSON]
    Life[lifecycle - como cria a instancia]
    Id[identification - Lookup ObjectId AOR]
    Http[protocol/http]
    Tcp[protocol/tcp]
    Int[interceptor - log latencia]
  end

  subgraph app [time-app = a aplicacao]
    GA[GatewayApp]
    GTS[GatewayTimeService]
    IR[InstanceRegistry + Heartbeat]
    IA[InstanceApp]
    ZTS[ZoneTimeService]
  end

  GA --> Mid
  GTS --> Mid
  IA --> Mid
  ZTS --> Mid
```

**Regra de ouro**

- Pasta `middleware-core` → padrões da disciplina (Invoker, Marshaller…).
- Pasta `time-app` → negócio (horário, gateway, LB).

---

## 6. O que roda na demo completa

```mermaid
flowchart LR
  JMeter --> GW

  subgraph host [Sua maquina ou EC2]
    GW[GatewayApp :8080 HTTP / :9091 TCP / :9000 heartbeat]
    BR1[InstanceApp br-1 :9101]
    BR2[InstanceApp br-2 :9102]
    PT1[InstanceApp pt-1 :9201]
    PT2[InstanceApp pt-2 :9202]
  end

  GW -->|"escolhe BR viva e encaminha"| BR1
  GW --> BR2
  GW --> PT1
  GW --> PT2
  BR1 -.->|REGISTER HEARTBEAT| GW
  BR2 -.->|REGISTER HEARTBEAT| GW
  PT1 -.->|REGISTER HEARTBEAT| GW
  PT2 -.->|REGISTER HEARTBEAT| GW
```

Sem instâncias saudáveis, o gateway responde com **erro (HTTP 400)** — no JMeter a taxa de erro tende a ~100%. Com instâncias, faz load balance e encaminha.

---

## 7. Caminho de UM pedido HTTP (o mais importante)

Exemplo: `GET http://127.0.0.1:8080/time/br`

```mermaid
sequenceDiagram
  participant C as JMeter ou curl
  participant HTTP as HttpProtocolPlugin
  participant SRH as ServerRequestHandler
  participant INT as LoggingInterceptor
  participant INV as Invoker
  participant LIFE as LifecycleManager
  participant REG as RemoteObjectRegistry
  participant APP as GatewayTimeService
  participant BE as InstanceApp

  C->>HTTP: GET /time/br
  HTTP->>HTTP: parseia HTTP/1.0 vira Request
  HTTP->>SRH: handle(Request)
  SRH->>INT: before
  SRH->>INV: invoke(Request)
  INV->>REG: acha rota GET /time/{zone}
  INV->>LIFE: pega instancia do componente
  INV->>APP: now("br")
  APP->>APP: nextHealthy("br") no InstanceRegistry
  alt tem instancia viva
    APP->>BE: HTTP GET host:porta/time
    BE-->>APP: horario
    APP-->>INV: String horario
    INV-->>SRH: resultado
    SRH->>SRH: Marshaller vira JSON
    SRH->>INT: after
    SRH-->>HTTP: Response 200
    HTTP-->>C: corpo JSON
  else sem instancia
    APP-->>INV: IllegalArgumentException
    INV-->>SRH: ApplicationException
    SRH->>INT: after
    SRH-->>HTTP: Response 400
    HTTP-->>C: JSON de erro
  end
```

### Em português, passo a passo

1. **JMeter/curl** manda `GET /time/br` na 8080.
2. **`HttpProtocolPlugin`** (middleware) aceita o socket e monta um `Request`.
3. **`ServerRequestHandler`** chama interceptors e o Invoker.
4. **`RemoteObjectRegistry`** já sabe (desde o `register`) que  
   `GET /time/{zone}` → `GatewayTimeService.now`.
5. **`Invoker`** resolve `@PathParam("zone")` = `"br"` e faz `method.invoke`.
6. **`GatewayTimeService`** (app) escolhe uma instância BR no registry (LB).
7. Encaminha para a instância; se não houver saudável, lança erro (HTTP 400).
8. Volta: Marshaller → Response → bytes HTTP → cliente.

TCP é a mesma ideia: linha `TIME br` vira um `Request` GET `/time/br` e segue o mesmo Invoker.

---

## 8. Como as anotações entram nisso

```java
@RemoteComponent(name = "gateway-time", lifecycle = STATIC)
public class GatewayTimeService {

  @RemoteMethod(path = "/time/{zone}", httpMethod = GET)
  public String now(@PathParam("zone") String zone) { ... }
}
```

| Anotação | Significa |
|----------|-----------|
| `@RemoteComponent` | Esta classe é um objeto remoto |
| `@RemoteMethod` | Este método pode ser chamado pela rede nesta rota |
| `@PathParam` | Este argumento vem do path (`br` em `/time/br`) |

No `mw.register(obj)`, o middleware **varre** esses métodos e monta a tabela de rotas. Sem anotação, o método não existe para a rede.

---

## 9. Critérios da nota ↔ onde está no código

```mermaid
flowchart TB
  MC[Modelo de componentes 2.0] --> Ann["annotations + ZoneTimeService/GatewayTimeService"]
  BR[Basic Remoting 1.0] --> Core["handler + invoker + marshaller + exceptions"]
  EX[Extension 3.0] --> Ext["protocol/http + protocol/tcp + LoggingInterceptor"]
  LC[Lifecycle 1.5] --> Life["LifecycleManager"]
  ID[Identification 1.0] --> Ident["ObjectId + AOR + Lookup + /lookup"]
  AS[Asynchrony 1.5] --> Depois["adiado de proposito"]
```

---

## 10. O que você sobe na apresentação

### Mínimo (já impressiona)

```powershell
mvn clean install
# classpath como no README
java ... br.imd.ufrn.app.GatewayApp
java ... br.imd.ufrn.app.instance.InstanceApp br br-1 9101
curl http://127.0.0.1:8080/time/br
# JMeter carga-http.jmx
```

### Completo (igual espírito do TP1)

1. Terminal: `GatewayApp`
2. Terminais: `InstanceApp br br-1 9101`, `br-2`, `pt-1`, `pt-2`
3. JMeter batendo no gateway
4. Se pedirem “mata uma instância”: **Ctrl+C** no terminal dela  
   → heartbeat para → gateway remove → LB usa as outras (erro sobe, sem tender a 100%)
5. Se pedirem “mata todas”: derruba as restantes → erro tende a ~100%; religa → erro cai

> Ops `POST/DELETE` na **8081** (`InstanceManagerServer`) sobe/derruba instâncias como no TP1.
> Durante o JMeter: `DELETE {"id":"br-1"}` — o heartbeat para e o gateway tira a réplica do LB.

---

## 11. Checklist “eu entendi?”

Se você souber responder isto, está ok para a disciplina:

1. **Por que dois módulos?** Um é a plataforma (JAR), outro é a app — exigência do PDF.  
2. **JMeter chama quem?** `GatewayApp` do TP2, porta 8080.  
3. **Onde está a regra de negócio?** `time-app` (`GatewayTimeService`, `ZoneTimeService`).  
4. **Onde está a “mágica” de rede?** `middleware-core`.  
5. **O `api-gateway` entra na demo?** Não. É só o ancestral do TP1.  
6. **O que é Invoker?** Quem acha o método anotado e chama com reflexão.  

---

## 12. Frase para guardar

> TP1 = eu *era* o middleware (sockets em tudo).  
> TP2 = eu *escrevi* o middleware e a app só declara objetos remotos.

Se travar de novo, volte na **seção 7** (caminho do `GET /time/br`). É o coração do trabalho.
