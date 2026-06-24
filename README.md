# loc (Localização)

Serviço de **rastreamento e localização ao vivo** da Trilha. Recebe a telemetria GPS do app durante uma trilha, persiste a sessão e seus pontos, calcula a distância real (Haversine) e simplifica o trajeto ao finalizar (Douglas-Peucker). Difunde a posição ao vivo para os amigos via WebSocket, com fan-out entre instâncias pelo Redis.

- **Porta:** `8082`
- **Pacote raiz:** `com.trisha.Loc.loc`
- **Banco:** PostgreSQL + **PostGIS** `trilha_localizacao` (porta `5433`)

## O que faz

- **Sessões de rastreamento**: iniciar, finalizar, cancelar — uma sessão ativa por usuário/caminho.
- **Pontos GPS**: registro sequencial; na finalização, **simplifica o trajeto** (Douglas-Peucker) removendo pontos redundantes de trechos retilíneos, preservando a forma da trilha. A distância total é calculada **antes** da simplificação, com todos os pontos.
- **Término automático** (opcional): avisa (sem finalizar sozinho) quando o usuário volta para perto do ponto inicial, dentro de um raio configurável.
- **Tempo real**: ingestão da telemetria via **MQTT** (broker Mosquitto, resiliente a sinal fraco — o cliente bufferiza offline); difusão via **WebSocket/STOMP** (`/ws-localizacao`, tópico `/topic/sessao/{id}`), com **Redis Pub/Sub** ligando as instâncias.
- **Visibilidade** da sessão (`PUBLICO`/`AMIGOS`/`PRIVADO`): o SUBSCRIBE é autorizado conforme a visibilidade — `AMIGOS` consulta o serviço APP (propagando o Bearer capturado no CONNECT).

## Stack

Spring Boot 4.0.6 · Java 21 · Spring Data JPA + **Hibernate Spatial (PostGIS)** · WebSocket/STOMP · Spring Data Redis (Pub/Sub) · Spring Integration MQTT + Eclipse Paho · OAuth2 Resource Server · Flyway · Lombok · logs JSON.

## Infra (compose.yaml)

| Serviço | Imagem | Porta |
|---|---|---|
| PostGIS | `postgis/postgis:16-3.4` | `5433` |
| Redis | `redis:7-alpine` | `6379` |
| Mosquitto (MQTT) | `eclipse-mosquitto:2` | `1883` |

Em **dev**, `spring-boot-docker-compose` sobe tudo automaticamente. Em produção há orientação de **ACL no Mosquitto** (credencial por device e restrição de tópico) — ver `mosquitto/`.

## Como rodar

```bash
export JAVA_HOME=/caminho/para/jdk-21   # requer JDK 21

# variáveis: DB_USERNAME, DB_PASSWORD, REDIS_HOST, MQTT_BROKER_URL,
#            JWKS_URI (default http://localhost:8080/oauth2/jwks), SERVICO_APP_URL
./gradlew bootRun
```

## Principais endpoints

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/localizacao/sessao` | inicia sessão |
| `POST` | `/localizacao/ponto` | registra ponto GPS |
| `PATCH` | `/localizacao/sessao/{id}/finalizar` | finaliza (calcula distância + simplifica) |
| `PATCH` | `/localizacao/sessao/{id}/cancelar` | cancela |
| `GET` | `/localizacao/sessao/caminho/{caminhoId}` | sessão de um caminho |
| `GET` | `/localizacao/pontos/sessao/{sessaoId}` | pontos da sessão |
| WS | `/ws-localizacao` → `/topic/sessao/{id}` | posição ao vivo |

## Testes

```bash
./gradlew test             # unitários (inclui Douglas-Peucker, MQTT handler, autorização do subscribe)
./gradlew integrationTest  # integração com PostGIS real (Testcontainers)
```

## Convenções

Identificadores do código em **inglês**; **JSON, rotas e colunas do banco em português**. Tópicos STOMP, canais Redis e nomes de propriedade MQTT são contrato e permanecem como estão. Correlação por `X-Trace-Id`.

> Parte da arquitetura da Trilha: Cadastro (8080) · APP (8081) · **loc (8082)** · midia (8083) · BFF (8090).
