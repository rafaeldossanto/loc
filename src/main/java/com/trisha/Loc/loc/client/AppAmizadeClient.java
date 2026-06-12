package com.trisha.Loc.loc.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Consulta de amizade no servico APP, para a visibilidade AMIGOS. Propaga o
 * Bearer do assinante (capturado no CONNECT do WebSocket).
 */
@Component
@RequiredArgsConstructor
public class AppAmizadeClient {

    private final RestClient appRestClient;

    public boolean saoAmigos(String usuarioA, String usuarioB, String bearerToken) {
        Boolean resultado = appRestClient.get()
                .uri(b -> b.path("/amizade/sao-amigos")
                        .queryParam("a", usuarioA)
                        .queryParam("b", usuarioB)
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(resultado);
    }
}
