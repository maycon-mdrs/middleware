# Ambiente AWS — middleware (TP2)

Mesma ideia do TP1 (`api-gateway/ambiente-aws.md`): **1 EC2** com gateway + instâncias BR/PT no mesmo host.  
JMeter roda no **PC** apontando para o IP público.

Preencha a tabela depois de criar/ligar a instância.

---

## Ambiente (reuso do TP1)

Confirmado: a EC2 do 1º trabalho **ainda existe**. Reusamos a mesma máquina (parar o `api-gateway` antes de subir o middleware — mesmas portas).

| Campo | Valor |
| ----- | ----- |
| Instância | UNIDADE1 (`i-0dfac3592a81dc46e`) |
| Tipo | `t3.micro` |
| Região | `us-east-2` |
| SO | Amazon Linux 2023 |
| Usuário SSH | `ec2-user` |
| IP privado | `172.31.39.59` |
| IP público | Elastic IP `3.146.239.232` |
| Chave | `api-gateway/my-key.pem` (não versionar) |
| Security Group | `launch-wizard-1` (`sg-0e4e6ed231cc5eab6`) |
| Topologia | 1 EC2: `GatewayApp` + 4× `InstanceApp` (+ ops opcional) em `127.0.0.1` |
| Repo na VM | `~/middleware` — branch `main` |

---

## O que vamos fazer (roteiro)

1. Autenticar AWS CLI na conta pessoal (`aws login` / SSO / access keys).
2. Ligar ou criar 1 EC2 Amazon Linux 2023 em `us-east-2` (pode ser a mesma máquina do TP1 se ainda existir).
3. Security Group: SSH + portas do middleware (abaixo).
4. Na VM: Java 21 + Maven + `git clone` + `mvn clean install`.
5. Subir gateway + 4 instâncias com `nohup`.
6. No PC: JMeter com `-JgatewayHost=<IP_PUBLICO>`.
7. Demo kill: `pkill` / ops `DELETE` → erro sobe; religar → erro cai.
8. Medir knee/usable (ver [aws-e-capacidade.md](aws-e-capacidade.md)).

---

## Portas (Security Group)

| Porta | Uso | Quem acessa |
| ----- | --- | ----------- |
| 22/TCP | SSH | seu IP |
| 8080/TCP | Gateway HTTP (JMeter) | seu IP / 0.0.0.0/0 na demo |
| 9091/TCP | Gateway TCP | seu IP (se testar TCP) |
| 9000/TCP | Heartbeat | **só interno** (mesmo host; não precisa abrir) |
| 9101–9102, 9201–9202 | Instâncias HTTP | **só interno** (gateway faz forward em localhost) |
| 8081/TCP | Ops (`InstanceManagerServer`) | só se chamar de fora; na demo os `curl` são na própria VM |

Na prática, para a banca: **22 + 8080** (+ **9091** se mostrar TCP). O resto fica em loopback.

---

## 1. Permissões da chave (PowerShell, no PC)

```powershell
$k = "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\my-key.pem"
icacls.exe $k /reset
icacls.exe $k /GRANT:R "$($env:USERNAME):R"
icacls.exe $k /inheritance:r
```

## 2. SSH (PowerShell, no PC)

```powershell
$k = "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\my-key.pem"
ssh -i $k ec2-user@<IP_PUBLICO>
```

## 3. Java 21 + Maven (bash, na VM)

```bash
sudo dnf update -y
sudo dnf install -y java-21-amazon-corretto-devel git
sudo dnf install -y maven

export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -version
```

O Maven do Amazon Linux às vezes aponta para Java 17 — mantenha o `JAVA_HOME` acima em toda sessão (ou coloque no `~/.bashrc`).

## 4. Clonar e compilar (bash, na VM)

```bash
cd ~
git clone https://github.com/maycon-mdrs/middleware.git
cd ~/middleware
git switch main
git pull

export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
export PATH="$JAVA_HOME/bin:$PATH"
mvn clean install
```

Classpath (Linux — `:` e não `;`):

```bash
export CP="time-app/target/classes:middleware-core/target/classes:$HOME/.m2/repository/org/json/json/20250517/json-20250517.jar"
```

## 5. Subir gateway + instâncias (bash, na VM)

### Automático no boot (recomendado)

Depois de `mvn clean install` pelo menos uma vez:

```bash
cd ~/middleware
git pull
bash scripts/install-systemd.sh
```

Isso instala o serviço `middleware` (systemd): em todo **Start** da EC2 sobem gateway + 4 instâncias + ops.

```bash
sudo systemctl status middleware
sudo systemctl restart middleware   # sobe de novo
sudo systemctl stop middleware      # derruba tudo
```

Scripts: [`scripts/start-all.sh`](../scripts/start-all.sh), [`scripts/stop-all.sh`](../scripts/stop-all.sh).

### Manual (sem systemd)

```bash
cd ~/middleware
bash scripts/start-all.sh
# ou: bash scripts/stop-all.sh
```

Encerre processos antigos (TP1 ou TP2) que segurem porta:

```bash
pkill -f 'br.imd.ufrn' || true
sleep 2
ps aux | grep br.imd.ufrn | grep -v grep
```

Conferência:

```bash
ps aux | grep br.imd.ufrn | grep -v grep
curl -s http://127.0.0.1:8080/time/br
curl -s http://127.0.0.1:8080/registry
tail -n 30 gateway.log
```

Esperado: **HTTP 200** em `/time/br` e `/time/pt`; registry com `br-1`, `br-2`, `pt-1`, `pt-2`.

Sem instâncias saudáveis → **HTTP 400** (necessário para kill total no JMeter).

## 5b. Postman (no PC)

Collection: [`postman/middleware.postman_collection.json`](../postman/middleware.postman_collection.json) — ver também [`postman/README.md`](../postman/README.md).

- Importar no Postman.
- Local / SSH na VM: `opsBaseUrl=http://127.0.0.1:8081` e `gatewayBaseUrl=http://127.0.0.1:8080`.
- Do PC para a EC2: troque as variáveis para `http://<IP_PUBLICO>:8081` e `:8080` (SG com essas portas). O IP está na tabela no topo deste arquivo.

## 6. Kill / religar (na VM, com JMeter rodando no PC)

Parcial (erro sobe, não tende a 100%):

```bash
pkill -f 'InstanceApp br br-1' || true
# ou via ops:
# curl -X DELETE http://127.0.0.1:8081/instances -H 'Content-Type: application/json' -d '{"id":"br-1"}'
```

Total (erro → ~100%):

```bash
pkill -f 'InstanceApp' || true
```

Religar:

```bash
cd ~/middleware
export CP="time-app/target/classes:middleware-core/target/classes:$HOME/.m2/repository/org/json/json/20250517/json-20250517.jar"
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp br br-1 9101 > br-1.log 2>&1 &
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp br br-2 9102 > br-2.log 2>&1 &
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp pt pt-1 9201 > pt-1.log 2>&1 &
nohup java -cp "$CP" br.imd.ufrn.app.instance.InstanceApp pt pt-2 9202 > pt-2.log 2>&1 &
```

## 7. JMeter (PowerShell, no PC)

Com o gateway no ar na EC2:

```powershell
cd c:\Users\mayco\OneDrive\Documentos\UF\middleware
jmeter -n -t jmeter\carga-http.jmx "-JgatewayHost=<IP_PUBLICO>" "-Jusers=8" "-Jramp=5" "-Jduration=30"
```

TCP (SG com 9091 aberto):

```powershell
jmeter -n -t jmeter\carga-tcp.jmx "-JgatewayHost=<IP_PUBLICO>" "-Jusers=8" "-Jramp=5" "-Jduration=30"
```

---

## Diferenças vs TP1 (`api-gateway`)

| | TP1 | TP2 (este repo) |
| -- | --- | --- |
| Main gateway | `br.imd.ufrn.Main gateway` | `br.imd.ufrn.app.GatewayApp` |
| Instâncias | `Main br br-1 9101` | `InstanceApp br br-1 9101` |
| Classpath | `target/classes` | `time-app` + `middleware-core` + `org.json` |
| Rede no negócio | sockets na mão | anotações + Invoker no JAR |
| Kill total | workers mortos → erro | sem réplica saudável → **400** (sem fallback local) |

---

## Erros “sem sentido” no JMeter (mesmo do TP1)

Sintoma: por volta de **20–30s** o TPS de sucesso cai, failure sobe em BR e PT juntos; no Sumário Máx. ≈ **2000–2015 ms** e Error% ~10%.

Causa principal no código (não era só tamanho da VM):

1. **`HttpURLConnection` no forward** — keep-alive/pool sob carga deixava conexões “zumbis” e estourava read timeout de 2s. Corrigido: forward com **Socket + Connection: close** (igual `HttpForwarder` do TP1) e read timeout **5s**; 1 retry em outra réplica.
2. **`System.out` por request** — interceptor em gateway+instâncias serializava threads. Gateway só amostra; instâncias sem log por padrão.
3. Heartbeat timeout folgado (**20s**).

Depois de `git pull` + `mvn clean install` + restart, o Sumário saudável deve ficar perto de **0%** erro com `users=8`.

## Checklist rápido

- [ ] EC2 ligada + Elastic IP anotado nesta página
- [ ] SG: 22, 8080 (+ 9091 se TCP)
- [ ] Java 21 + Maven + `mvn clean install` OK
- [ ] 5 processos (gateway + 4 instâncias); `/time/br` = 200
- [ ] JMeter do PC com 0% erro
- [ ] Kill parcial / total / religar batendo com o Summary Report
- [ ] Tabela knee/usable preenchida ([aws-e-capacidade.md](aws-e-capacidade.md))
