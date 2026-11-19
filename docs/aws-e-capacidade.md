# AWS e capacidade (knee / usable)

## Deploy

Passo a passo completo (SSH, Java, clone, `nohup`, kill, JMeter): **[ambiente-aws.md](ambiente-aws.md)**.

Resumo: **1 EC2** Amazon Linux 2023 (mesma ideia do TP1), gateway + instâncias no host, JMeter no PC com `-JgatewayHost=<IP_PUBLICO>`.

## Knee capacity e Usable capacity

Repita a carga HTTP (`jmeter/carga-http.jmx`) aumentando `users` (ex.: 5, 8, 10, 15, 20, 30…):

| users | throughput (req/s) | latência p95 | erros % |
|-------|--------------------|--------------|---------|
| …     | …                  | …            | …       |

- **Knee capacity:** ponto em que o throughput para de crescer linearmente (ou a latência sobe rápido) — o “joelho” da curva.
- **Usable capacity:** tipicamente ~70–80% do knee (margem segura de operação), como no TP1.

Na demo de apresentação, rode abaixo do knee com **0% erro**; depois faça kill parcial/total.

Registre gráficos (throughput × users, latência × users) em `docs/figuras/` na apresentação.
