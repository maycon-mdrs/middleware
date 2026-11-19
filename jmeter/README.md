# JMeter — Middleware TP2 (avaliação)

Mesmo padrão do TP1 (`api-gateway/jmeter`): variáveis no Test Plan, Sumário, Árvore, TPS e Response Times (plugins jp@gc).

O middleware **não tem UDP** — só HTTP `:8080` e TCP `:9091`.

## Planos

| Arquivo | Uso |
|---------|-----|
| [`carga-completa.jmx`](carga-completa.jmx) | TCP → HTTP em sequência (avaliação) |
| [`carga-tcp.jmx`](carga-tcp.jmx) | **Só TCP** — prints e knee isolados |
| [`carga-http.jmx`](carga-http.jmx) | **Só HTTP** |
| [`tutorial-gui.md`](tutorial-gui.md) | Passo a passo na interface gráfica |

### `carga-completa.jmx`

```text
Middleware TP2 — TCP + HTTP
 ├── variáveis: gatewayHost, users, ramp, duration
 ├── 1 — TCP :9091     → Sumário TCP + Árvore TCP
 ├── 2 — HTTP :8080    → Sumário HTTP + Árvore HTTP
 ├── jp@gc - Transactions per Second
 └── jp@gc - Response Times Over Time
```

Roda em sequência. Para demo sem erro: **`users=8`**.

### Planos isolados (`carga-tcp` / `carga-http`)

Mesmas variáveis e listeners (Sumário, Árvore, TPS, Response Times). Ideal para prints por protocolo.

## Pré-requisitos

- JMeter 5.6+ (ex.: `C:\apache-jmeter-5.6.3`)
- Plugins **jp@gc** (Transactions per Second, Response Times Over Time) — mesmos do TP1
- App no ar na EC2 — ver [`../docs/ambiente-aws.md`](../docs/ambiente-aws.md)
- Security Group: **8080/TCP**, **9091/TCP** (+ SSH)

## Variáveis (Test Plan)

| Nome | Default | Efeito |
|------|--------:|--------|
| `gatewayHost` | `127.0.0.1` | IP público da EC2 na demo |
| `users` | 8 | threads (**>5** e &lt; knee; usable) |
| `ramp` | 5 | segundos para subir os users |
| `duration` | 30 | segundos de carga (demo: 60) |

Na GUI: clique no Test Plan → edite `gatewayHost` para o IP da UNIDADE1 (tabela em `docs/ambiente-aws.md`).

CLI: `-J` via `__P`:

```powershell
jmeter -n -t carga-http.jmx "-JgatewayHost=<IP_PUBLICO>" "-Jusers=8" "-Jramp=5" "-Jduration=60"
```

## Como executar (GUI) — prints

1. Abrir o `.jmx` do protocolo (ou o completo).
2. Conferir `gatewayHost` (IP público atual).
3. **Saudável:** `users=8`, `duration=60` → Error % ≈ 0.
4. Abrir **Sumário HTTP** / **Sumário TCP** e dar **Start**.
5. Print do Sumário (Error %, Vazão) + TPS se quiser.

Detalhe: [tutorial-gui.md](tutorial-gui.md).

## Demo de tolerância a falhas

Com `carga-http` ou `carga-completa` em `users=8`:

1. Sumário com Error ≈ 0.
2. Postman / ops: `DELETE` `br-1` → erro sobe (não ~100%).
3. `DELETE` todas → erro ~100%.
4. `POST` de novo → erros caem.

## Knee / Usable

Template: [`knee-usable-template.csv`](knee-usable-template.csv) (valores do TP1 como referência; meça de novo no middleware se a banca pedir).

| | TCP / HTTP |
|--|------------|
| **Usable (demo)** | **8** |
| Degradação típica | Error >1% ≈ **15** users |
