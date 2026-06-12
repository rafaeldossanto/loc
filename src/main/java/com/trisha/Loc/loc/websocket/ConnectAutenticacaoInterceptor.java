package com.trisha.Loc.loc.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Autentica o CONNECT do WebSocket: valida o Bearer dos native headers com a
 * mesma chave publica do resource server (JWKS), seta o Principal (id do usuario)
 * e guarda o token nos atributos da sessao para a checagem de amizade no SUBSCRIBE.
 */
@Component
@RequiredArgsConstructor
public class ConnectAutenticacaoInterceptor implements ChannelInterceptor {

    public static final String ATRIBUTO_TOKEN = "token";

    private final JwtDecoder jwtDecoder;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (isNull(authorization) || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Token ausente no CONNECT");
        }

        String token = authorization.substring(7);
        Jwt jwt = jwtDecoder.decode(token);

        Principal principal = jwt::getSubject;
        accessor.setUser(principal);

        Map<String, Object> atributos = accessor.getSessionAttributes();
        if (atributos != null) {
            atributos.put(ATRIBUTO_TOKEN, token);
        }
        return message;
    }
}
