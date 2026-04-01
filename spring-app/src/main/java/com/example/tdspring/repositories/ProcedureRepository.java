package com.example.tdspring.repositories;

import com.example.tdspring.models.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProcedureRepository extends JpaRepository<Procedure, Long> {
    List<Procedure> findAllByOrderByCreatedAtAsc();
    List<Procedure> findByCategoryOrderByCreatedAtAsc(String category);
}