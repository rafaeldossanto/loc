package com.trisha.Loc.loc;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Teste de integracao: sobe o contexto Spring completo contra um
 * PostGIS real provisionado pelo Testcontainers. A imagem postgis/postgis
 * ja vem com a extensao espacial habilitada, compativel com o dialect
 * spatial configurado no servico.
 */
@Tag("integracao")
@SpringBootTest
@Testcontainers
class ApplicationIT {

    private static final DockerImageName POSTGIS_IMAGE = DockerImageName
            .parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgis = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("trilha_localizacao");

    @DynamicPropertySource
    static void jpaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Test
    void contextLoads() {
    }
}
