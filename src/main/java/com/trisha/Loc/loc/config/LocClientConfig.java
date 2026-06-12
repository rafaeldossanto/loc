package com.trisha.Loc.loc.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Client HTTP para o servico APP — usado pela visibilidade AMIGOS (consulta de
 * amizade). O Bearer do assinante e propagado por chamada (nao aqui).
 */
@Configuration
public class LocClientConfig {

    @Bean
    public RestClient appRestClient(@Value("${servicos.app:http://localhost:8081}") String appUrl) {
        return RestClient.builder().baseUrl(appUrl).build();
    }
}
