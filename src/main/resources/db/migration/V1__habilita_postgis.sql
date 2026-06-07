-- Garante que a extensao PostGIS esteja habilitada no banco do servico de
-- Localizacao. Necessaria para os tipos/funcoes espaciais usados no rastreamento
-- de trilhas. A imagem postgis/postgis ja traz a extensao instalada; este
-- comando a habilita de forma idempotente.
CREATE EXTENSION IF NOT EXISTS postgis;
