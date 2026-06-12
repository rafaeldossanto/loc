package com.trisha.Loc.loc.websocket;

import com.trisha.Loc.loc.client.AppAmizadeClient;
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

import java.security.Principal;
import java.util.Map;

import static java.util.Objects.isNull;

/**
 * Autoriza o SUBSCRIBE a /topic/sessao/{id} conforme a visibilidade:
 *  - PUBLICO: qualquer um acompanha;
 *  - PRIVADO: ninguem (nem o dono via topico) — so o proprio app local;
 *  - AMIGOS: o proprio dono ou quem for amigo dele (consulta ao servico APP,
 *    propagando o Bearer capturado no CONNECT).
 */
@Component
@RequiredArgsConstructor
public class SubscribeAutorizacaoInterceptor implements ChannelInterceptor {

    private static final String PREFIXO_TOPICO_SESSAO = "/topic/sessao/";

    private final SessaoRastreamentoRepository sessaoRepository;
    private final AppAmizadeClient appAmizadeClient;

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
            autorizarAmigos(accessor, sessao);
        }
        return message;
    }

    private void autorizarAmigos(StompHeaderAccessor accessor, SessaoRastreamento sessao) {
        Principal user = accessor.getUser();
        if (isNull(user)) {
            throw new IllegalStateException("Assinante nao autenticado");
        }

        String assinante = user.getName();
        if (assinante.equals(sessao.getUsuarioId())) {
            return;
        }

        if (!appAmizadeClient.saoAmigos(assinante, sessao.getUsuarioId(), token(accessor))) {
            throw new IllegalStateException("Apenas amigos podem acompanhar esta sessao");
        }
    }

    private String token(StompHeaderAccessor accessor) {
        Map<String, Object> atributos = accessor.getSessionAttributes();
        return isNull(atributos) ? null : (String) atributos.get(ConnectAutenticacaoInterceptor.ATRIBUTO_TOKEN);
    }
}
