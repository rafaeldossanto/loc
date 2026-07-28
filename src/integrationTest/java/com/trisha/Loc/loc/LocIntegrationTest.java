package com.trisha.Loc.loc;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base dos testes de integracao do loc: provisiona a infra que o contexto exige
 * para subir e a compartilha entre todas as subclasses.
 *
 * <p>Sao dois containers, e os dois sao necessarios:
 * <ul>
 *   <li><b>PostGIS</b> — a imagem ja traz a extensao espacial que o dialect
 *       spatial e o indice GiST do bbox exigem;</li>
 *   <li><b>Redis</b> — o fan-out do tempo real usa Pub/Sub, e o
 *       {@code RedisMessageListenerContainer} e um bean de ciclo de vida: ele
 *       conecta durante o start do contexto e, sem broker, derruba o
 *       {@code @SpringBootTest} inteiro.</li>
 * </ul>
 *
 * <p>O MQTT NAO precisa de broker aqui: o adapter e configurado com
 * {@code automaticReconnect}, entao a falha de conexao inicial so agenda nova
 * tentativa em vez de abortar o contexto. A ingestao MQTT tem cobertura propria
 * nos testes de unidade do handler.
 *
 * <p>Os containers sao <b>estaticos e iniciados uma unica vez</b> (padrao
 * singleton do Testcontainers), em vez de anotados com {@code @Container} em cada
 * classe. Com {@code @Container} cada classe de teste subia e derrubava o proprio
 * PostGIS — a imagem mais pesada do projeto. Compartilhando, a infra sobe uma vez
 * por execucao e o Spring ainda reaproveita o mesmo ApplicationContext entre as
 * classes, porque as propriedades registradas sao identicas. O Ryuk derruba tudo
 * ao fim da JVM.
 */
@Tag("integracao")
@SpringBootTest
public abstract class LocIntegrationTest {

    private static final DockerImageName POSTGIS_IMAGE = DockerImageName
            .parse("postgis/postgis:16-3.4")
            .asCompatibleSubstituteFor("postgres");

    private static final PostgreSQLContainer<?> POSTGIS = new PostgreSQLContainer<>(POSTGIS_IMAGE)
            .withDatabaseName("trilha_localizacao");

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGIS.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void infraProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGIS::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGIS::getUsername);
        registry.add("spring.datasource.password", POSTGIS::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }
}
