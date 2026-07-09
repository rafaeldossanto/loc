-- Garante a extensao espacial mesmo quando o V1 foi pulado pelo baseline
-- (banco que ja existia quando o Flyway entrou): CREATE EXTENSION e idempotente.
CREATE EXTENSION IF NOT EXISTS postgis;

-- Schema base versionado do servico de Localizacao. Ate aqui as tabelas so
-- nasciam pelo ddl-auto=update do Hibernate; a partir deste V4 um banco novo
-- sobe inteiro pelo Flyway (IF NOT EXISTS mantem compatibilidade com bancos
-- que o Hibernate ja criou). Producao troca DDL_AUTO para validate.

CREATE TABLE IF NOT EXISTS sessao_rastreamento (
    id                        VARCHAR(255) PRIMARY KEY,
    caminho_id                VARCHAR(255) NOT NULL,
    usuario_id                VARCHAR(255) NOT NULL,
    status                    VARCHAR(255) NOT NULL,
    termino_automatico        BOOLEAN      NOT NULL,
    distancia_termino_metros  DOUBLE PRECISION NOT NULL,
    visibilidade              VARCHAR(255) NOT NULL DEFAULT 'PRIVADO',
    distancia_total_km        DOUBLE PRECISION,
    iniciada_em               TIMESTAMP    NOT NULL,
    finalizada_em             TIMESTAMP,
    trace_id                  VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_sessao_caminho ON sessao_rastreamento (caminho_id);
CREATE INDEX IF NOT EXISTS idx_sessao_usuario ON sessao_rastreamento (usuario_id);

CREATE TABLE IF NOT EXISTS ponto_gps (
    id             VARCHAR(255) PRIMARY KEY,
    sessao_id      VARCHAR(255) NOT NULL REFERENCES sessao_rastreamento (id),
    latitude       DOUBLE PRECISION NOT NULL,
    longitude      DOUBLE PRECISION NOT NULL,
    altitude       DOUBLE PRECISION,
    precisao       DOUBLE PRECISION,
    velocidade     DOUBLE PRECISION,
    ordem          INTEGER   NOT NULL,
    registrado_em  TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ponto_gps_sessao ON ponto_gps (sessao_id);

-- Indice espacial (GiST/PostGIS) da consulta por bounding box do mapa.
-- A MESMA expressao e usada no WHERE da query nativa — e o que permite ao
-- planner usar o indice em vez de varrer a tabela a cada pan/zoom.
CREATE INDEX IF NOT EXISTS idx_ponto_gps_geo ON ponto_gps
    USING gist (ST_SetSRID(ST_MakePoint(longitude, latitude), 4326));
