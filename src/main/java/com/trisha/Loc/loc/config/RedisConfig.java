package com.trisha.Loc.loc.config;

import com.trisha.Loc.loc.websocket.GpsPointRedisSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Liga o Redis Pub/Sub ao broker STOMP: cada instancia assina o canal de pontos
 * e, ao receber, reentrega aos seus assinantes WebSocket locais. E isso que faz
 * um amigo conectado na instancia B receber a posicao publicada na instancia A.
 */
@Configuration
public class RedisConfig {

    public static final String POINTS_CHANNEL = "localizacao.pontos";

    @Bean
    public ChannelTopic pointsChannel() {
        return new ChannelTopic(POINTS_CHANNEL);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            GpsPointRedisSubscriber subscriber) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(subscriber, pointsChannel());
        return container;
    }
}
