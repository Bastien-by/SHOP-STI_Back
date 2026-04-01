package com.example.tdspring.repositories;

import com.example.tdspring.models.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, Long> {
    List<Procedure> findAllByOrderByCreatedAtAsc();
}