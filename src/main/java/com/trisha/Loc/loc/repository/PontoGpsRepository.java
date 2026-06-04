package com.trisha.Loc.loc.repository;

import com.trisha.Loc.loc.entity.PontoGps;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PontoGpsRepository extends JpaRepository<PontoGps, String> {

    List<PontoGps> findBySessaoIdOrderByOrdemAsc(String sessaoId);

    int countBySessaoId(String sessaoId);
}
