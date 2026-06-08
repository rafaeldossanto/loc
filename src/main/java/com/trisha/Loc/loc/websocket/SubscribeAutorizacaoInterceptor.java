package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.enums.VisibilidadeSessao;
import com.trisha.Loc.loc.repository.SessaoRastreamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import static java.util.Objects.isNull;

/**
 * Autoriza (ou nao) o SUBSCRIBE a /topic/sessao/{id} conforme a visibilidade da
 * sessao:
 *  - PUBLICO: qualquer um acompanha ao vivo;
 *  - PRIVADO: ninguem acompanha ao vivo;
 *  - AMIGOS: TODO — liberar apenas amigos quando o auth do BFF existir (a
 *    identidade do assinante ainda nao chega aqui, entao bloqueia por seguranca).
 *
 * A autorizacao de PUBLICAR pontos (so o dono da sessao) tambem depende do auth
 * e fica para a mesma etapa.
 */
@Component
@RequiredArgsConstructor
public class SubscribeAutorizacaoInterceptor implements ChannelInterceptor {

    private static final String PREFIXO_TOPICO_SESSAO = "/topic/sessao/";

    private final SessaoRastreamentoRepository sessaoRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (!StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            return message;
        }

        String destino = accessor.getDestination();
        if (isNull(destino) || !destino.startsWith(PREFIXO_TOPICO_SESSAO)) {
            return message;
        }

        String sessaoId = destino.substring(PREFIXO_TOPICO_SESSAO.length());
        SessaoRastreamento sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessao nao encontrada: " + sessaoId));

        if (VisibilidadeSessao.PRIVADO.equals(sessao.getVisibilidade())) {
            throw new IllegalStateException("Sessao privada — acompanhamento ao vivo nao permitido");
        }
        if (VisibilidadeSessao.AMIGOS.equals(sessao.getVisibilidade())) {
            // TODO: liberar apenas amigos quando houver auth (identidade do assinante
            // + consulta ao servico de amizade). Por ora, bloqueia por seguranca.
            throw new IllegalStateException("Sessao visivel apenas para amigos — login ainda nao implementado");
        }
        return message; // PUBLICO
    }
}
