package com.example.tdspring.services;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Procedure;
import com.example.tdspring.repositories.ProcedureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcedureService {

    private final ProcedureRepository procedureRepository;

    public List<Procedure> getAllProcedures() {
        return this.procedureRepository.findAllByOrderByCreatedAtAsc();
    }

    public Procedure updateProcedure(Procedure procedure) throws DBException, NotFoundException {
        Procedure existing;
        log.info("Service : Updating procedure ...");

        if (procedure.getId() != null) {
            existing = this.procedureRepository.findById(procedure.getId()).orElse(null);
            if (existing == null) {
                throw new NotFoundException("Could not find procedure with id : " + procedure.getId());
            }
        } else {
            existing = new Procedure();
        }

        existing.setTitle(procedure.getTitle());
        existing.setSubtitle(procedure.getSubtitle());
        existing.setSteps(procedure.getSteps());
        existing.setQuestions(procedure.getQuestions());
        existing.setPicture(procedure.getPicture());

        if (existing.getPicture() == null || existing.getPicture().equals("null")) {
            existing.setPicture(null);
        }

        try {
            return this.procedureRepository.save(existing);
        } catch (Exception e) {
            throw new DBException("Could not save procedure");
        }
    }

    public Procedure deleteProcedure(Long id) throws NotFoundException, DBException {
        Procedure existing = this.procedureRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new NotFoundException("Could not find procedure with id : " + id);
        }

        try {
            this.procedureRepository.delete(existing);
            return existing;
        } catch (Exception e) {
            throw new DBException("Could not delete procedure");
        }
    }
}