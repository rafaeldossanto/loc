-- Visibilidade do acompanhamento ao vivo da sessao (PUBLICO/AMIGOS/PRIVADO).
-- Default seguro PRIVADO: a localizacao so e exposta se o usuario optar.
-- IF NOT EXISTS evita conflito em dev, onde o ddl-auto=update do Hibernate
-- pode ter criado a coluna antes do Flyway rodar.
ALTER TABLE sessao_rastreamento
    ADD COLUMN IF NOT EXISTS visibilidade VARCHAR(20) NOT NULL DEFAULT 'PRIVADO';
