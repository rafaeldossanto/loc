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
public class AppFriendshipClient {

    private final RestClient appRestClient;

    public boolean areFriends(String userA, String userB, String bearerToken) {
        Boolean result = appRestClient.get()
                .uri(b -> b.path("/amizade/sao-amigos")
                        .queryParam("a", userA)
                        .queryParam("b", userB)
                        .build())
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(Boolean.class);
        return Boolean.TRUE.equals(result);
    }
}
