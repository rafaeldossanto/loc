# Configuracao do broker Mosquitto — servico de localizacao

## Visao geral

O servico `loc` consome pontos GPS via MQTT. O broker Mosquitto e responsavel pelo
roteamento das mensagens entre os dispositivos moveis e o consumidor Spring (loc-consumer).

Esta pasta contem as configuracoes do broker. Em producao, a ACL restringe cada
dispositivo movel a publicar apenas no topico do seu proprio usuario, impedindo
injecao de pontos de terceiros.

## Arquivos

| Arquivo | Descricao |
|---|---|
| `mosquitto.conf` | Configuracao principal do broker (DEV anonimo, PROD comentado) |
| `acl.example` | Exemplo de ACL com restricao por usuario e topico |

## Gerando o arquivo de senhas (producao)

```bash
# Instalar Mosquitto utilities
sudo apt-get install mosquitto-clients

# Criar o arquivo de senhas para o consumidor do servico loc
mosquitto_passwd -c /etc/mosquitto/passwd loc-consumer

# Adicionar um device para o usuario 42 (fazer na criacao de conta)
mosquitto_passwd /etc/mosquitto/passwd device-usuario-42
```

## Ativando a autenticacao em producao

1. No `mosquitto.conf`, descomente as linhas de producao e comente `allow_anonymous true`.
2. Copie `acl.example` para `/etc/mosquitto/acl` e ajuste os usuarios.
3. Gere as senhas com `mosquitto_passwd` conforme acima.
4. Reinicie o broker: `sudo systemctl restart mosquitto`.

## Topicos

| Topico | Publicador | Consumidor |
|---|---|---|
| `localizacao/ponto` | Dispositivo movel (device-<userId>) | loc-consumer (Spring) |

## Integracao com o Spring

O handler MQTT (`LocationMqttHandler`) ja valida se o `userId` do payload pertence
a sessao ativa, descartando pontos de usuarios nao donos da sessao. A ACL do broker
e uma camada complementar de defesa em profundidade.

## Variavel de ambiente

```env
MQTT_BROKER_URL=tcp://localhost:1883
MQTT_CLIENT_ID=loc-consumer
MQTT_TOPICO=localizacao/ponto
MQTT_QOS=1
```
