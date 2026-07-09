-- Guardado por to_regclass: ver comentario no V2 (banco novo cria tudo no V4).
DO $$
BEGIN
    IF to_regclass('sessao_rastreamento') IS NOT NULL THEN
        ALTER TABLE sessao_rastreamento ADD COLUMN IF NOT EXISTS trace_id VARCHAR(16);
    END IF;
END $$;
