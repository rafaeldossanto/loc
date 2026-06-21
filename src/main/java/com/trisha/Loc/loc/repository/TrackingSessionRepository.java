package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.TrackingSession;
import com.trisha.Loc.loc.model.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TrackingSessionRepository extends JpaRepository<TrackingSession, String> {

    List<TrackingSession> findByUserId(String userId);

    Optional<TrackingSession> findByPathId(String pathId);

    Optional<TrackingSession> findByUserIdAndStatus(String userId, SessionStatus status);
}
