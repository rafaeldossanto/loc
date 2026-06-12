package com.trisha.Loc.loc.config;

import com.trisha.Loc.loc.websocket.SubscribeAutorizacaoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Canal WebSocket/STOMP do tempo real da localizacao.
 *
 * - Conexao no endpoint /ws-localizacao (com fallback SockJS).
 * - O app PUBLICA pontos em /app/sessao/ponto e ASSINA /topic/sessao/{id} para
 *   receber a posicao ao vivo (a propria e a dos amigos).
 * - O broker simples e local a cada instancia; o fan-out ENTRE instancias e
 *   feito pelo Redis Pub/Sub (ver {@link RedisConfig}).
 * - O SUBSCRIBE passa pelo interceptor de autorizacao por visibilidade.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final com.trisha.Loc.loc.websocket.ConnectAutenticacaoInterceptor connectInterceptor;
    private final SubscribeAutorizacaoInterceptor autorizacaoInterceptor;

    public WebSocketConfig(com.trisha.Loc.loc.websocket.ConnectAutenticacaoInterceptor connectInterceptor,
                           SubscribeAutorizacaoInterceptor autorizacaoInterceptor) {
        this.connectInterceptor = connectInterceptor;
        this.autorizacaoInterceptor = autorizacaoInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-localizacao")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(connectInterceptor, autorizacaoInterceptor);
    }
}
