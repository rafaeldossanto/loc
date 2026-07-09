-- Visibilidade do acompanhamento ao vivo da sessao (PUBLICO/AMIGOS/PRIVADO).
-- Default seguro PRIVADO: a localizacao so e exposta se o usuario optar.
-- Guardado por to_regclass: em banco NOVO o Flyway roda ANTES do Hibernate
-- criar as tabelas (ddl-auto), entao o ALTER so se aplica se a tabela existe
-- (bancos antigos); em banco novo o V4 ja cria a coluna junto com o schema.
DO $$
BEGIN
    IF to_regclass('sessao_rastreamento') IS NOT NULL THEN
        ALTER TABLE sessao_rastreamento
            ADD COLUMN IF NOT EXISTS visibilidade VARCHAR(20) NOT NULL DEFAULT 'PRIVADO';
    END IF;
END $$;
