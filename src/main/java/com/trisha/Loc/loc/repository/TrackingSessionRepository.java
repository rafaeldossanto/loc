package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, String> {

    List<TrackingSession> findByUserId(String userId);

    Optional<TrackingSession> findByPathId(String pathId);

    Optional<TrackingSession> findByUserIdAndStatus(String userId, SessionStatus status);

    /** Sessoes ao vivo (EM_ANDAMENTO) — base da lista "quem esta trilhando agora". */
    List<TrackingSession> findByStatus(SessionStatus status);

    /**
     * Carrega a sessao com lock de escrita (SELECT ... FOR UPDATE) para serializar
     * o registro de pontos por sessao: o {@code count()+1} da ordem so e seguro se
     * duas gravacoes concorrentes (REST + MQTT) nao rodarem o count ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM TrackingSession s WHERE s.id = :id")
    Optional<TrackingSession> findByIdForUpdate(@Param("id") String id);
}
