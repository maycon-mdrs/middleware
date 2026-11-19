# Tutorial JMeter (GUI) — Middleware TP2

Igual ao fluxo do TP1 (`api-gateway`): abrir o `.jmx`, ajustar `gatewayHost`, olhar o **Sumário**, depois kill com Postman/ops.

---

## 0. Pré-requisitos

1. EC2 com gateway + 4 instâncias (+ ops 8081). Ver [docs/ambiente-aws.md](../docs/ambiente-aws.md).
2. No PC:

```powershell
curl http://<IP_PUBLICO>:8080/time/br
```

3. Plugins jp@gc instalados (mesmos do 1º trabalho): **Transactions per Second**, **Response Times Over Time**.
4. Postman com [`../postman/middleware.postman_collection.json`](../postman/middleware.postman_collection.json).

---

## 1. Abrir o plano (como no TP1)

1. Abra o **Apache JMeter**.
2. **File → Open…**
3. Escolha um destes (pasta `middleware\jmeter\`):

| Arquivo | Quando usar |
|---------|-------------|
| `carga-http.jmx` | Demo HTTP (mais comum) |
| `carga-tcp.jmx` | Só TCP |
| `carga-completa.jmx` | TCP depois HTTP (avaliação completa) |

4. Árvore esperada (HTTP):

```text
Middleware TP2 — HTTP :8080
 ├── HTTP :8080
 │    ├── HTTP GET /time/br
 │    ├── HTTP GET /time/pt
 │    ├── Sumário HTTP
 │    └── Árvore HTTP
 ├── jp@gc - Transactions per Second
 └── jp@gc - Response Times Over Time
```

Variáveis ficam no **Test Plan** (raiz), não num nó `vars` separado — igual ao TP1.

---

## 2. Configurar o host

1. Clique no **Test Plan** (raiz).
2. Em **User Defined Variables**:

| Name | Value |
|------|--------|
| `gatewayHost` | IP público da EC2 (ou `127.0.0.1` local) |
| `users` | `8` |
| `ramp` | `5` |
| `duration` | `60` (demo) |

3. Reabra o `.jmx` atualizado do Git: KeepAlive **off** + Implementation **Java**  
   (KeepAlive ligado com servidor `Connection: close` gera o buraco de erro aos ~20–30s).

4. Se um sample ficar vermelho: abra **Árvore HTTP** e anote a mensagem  
   (`Connection reset` / `Non HTTP` → cliente/KeepAlive; `Falha ao encaminhar` → forward; `Nenhuma instância` → heartbeat).

---

## 3. Rodar saudável

1. Clique em **Sumário HTTP** (ou **Sumário TCP**).
2. **Run → Start** (play verde).
3. Espere o `duration`.
4. **Error % ≈ 0,00%**.

Prints: Sumário + TPS (opcional).

---

## 4. Kill / religar (com JMeter rodando)

Use `duration=120` se precisar de mais tempo.

1. Postman **DELETE br-1** → Error % sobe (não ~100%).
2. **DELETE** as restantes → Error % ~ **100%**.
3. **POST** as quatro → Error % cai.

Ops na VM: `http://127.0.0.1:8081` (collection Postman).

---

## 5. Limpar entre runs

**Clear All** (borracha) antes de um novo Start.

---

## CLI (atalho)

```powershell
cd c:\Users\mayco\OneDrive\Documentos\UF\middleware\jmeter
jmeter -n -t carga-http.jmx "-JgatewayHost=<IP_PUBLICO>" "-Jusers=8" "-Jramp=5" "-Jduration=60"
jmeter -n -t carga-completa.jmx "-JgatewayHost=<IP_PUBLICO>" "-Jusers=8" "-Jramp=5" "-Jduration=30"
```

---

## Diferença vs TP1

| | TP1 | TP2 |
|--|-----|-----|
| Planos | HTTP, TCP, UDP, completa | HTTP, TCP, completa (**sem UDP**) |
| Listeners | Sumário, Árvore, TPS, RT | iguais |
| Variáveis | no Test Plan | iguais |
| Kill | ops / pkill | Postman ops `:8081` (mesmo espírito) |
