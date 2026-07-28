package com.trisha.Loc.loc;

import org.junit.jupiter.api.Test;

/**
 * Teste de integracao: sobe o contexto Spring completo contra a infra real
 * provisionada pela {@link LocIntegrationTest} (PostGIS + Redis). Valida que as
 * migrations aplicam, que o dialect spatial casa com a extensao espacial do banco
 * e que todos os beans do servico inicializam.
 */
class ApplicationIT extends LocIntegrationTest {

    @Test
    void contextLoads() {
    }
}
