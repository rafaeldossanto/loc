package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.SessaoRastreamento;
import com.trisha.Loc.loc.model.enums.StatusSessao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessaoRastreamentoRepository extends JpaRepository<SessaoRastreamento, String> {

    List<SessaoRastreamento> findByUsuarioId(String usuarioId);

    Optional<SessaoRastreamento> findByCaminhoId(String caminhoId);

    Optional<SessaoRastreamento> findByUsuarioIdAndStatus(String usuarioId, StatusSessao status);
}
