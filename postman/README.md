# Postman — Middleware TP2

## Importar

1. Abra o Postman → **Import**
2. Selecione [`middleware.postman_collection.json`](middleware.postman_collection.json)

## Variáveis da collection

| Variável | Local (padrão) | EC2 (do PC) |
|----------|----------------|-------------|
| `gatewayBaseUrl` | `http://127.0.0.1:8080` | `http://<IP_PUBLICO>:8080` |
| `opsBaseUrl` | `http://127.0.0.1:8081` | `http://<IP_PUBLICO>:8081` |

Para chamar o ops **do PC**, libere **8081** no Security Group. Na apresentação, o mais simples é SSH na VM e deixar `127.0.0.1`.

O IP público da UNIDADE1 está em [docs/ambiente-aws.md](../docs/ambiente-aws.md).

## Pastas

- **Gateway** — `/time/br`, `/time/pt`, `/registry`, `/lookup`
- **Ops — listar** — `GET /instances`
- **Ops — desligar** — `DELETE` por id (demo kill)
- **Ops — ligar** — `POST` para religar

## Demo com JMeter

1. JMeter no PC → `-JgatewayHost=<IP_PUBLICO>`
2. Postman: **DELETE br-1** (erro sobe; se matar todas → ~100%)
3. Postman: **POST br-1** (e as outras) → erro cai
