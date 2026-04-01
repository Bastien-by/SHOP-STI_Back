package com.example.tdspring.controllers;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Procedure;
import com.example.tdspring.services.ProcedureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/procedures")
@RequiredArgsConstructor
@Slf4j
public class ProcedureController {

    private final ProcedureService procedureService;

    @GetMapping
    public ResponseEntity<List<Procedure>> getProcedures() {
        return new ResponseEntity<>(this.procedureService.getAllProcedures(), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Procedure> postProcedure(
            @RequestParam("title") String title,
            @RequestParam("subtitle") String subtitle,
            @RequestParam("steps") String steps,
            @RequestParam("questions") String questions,
            @RequestParam(value = "picture", required = false) String picture
    ) {
        try {
            log.info("Creating a procedure ...");

            Procedure procedure = new Procedure();
            procedure.setTitle(title);
            procedure.setSubtitle(subtitle);
            procedure.setSteps(steps);
            procedure.setQuestions(questions);
            procedure.setPicture(picture);

            Procedure saved = this.procedureService.updateProcedure(procedure);
            return new ResponseEntity<>(saved, HttpStatus.CREATED);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Procedure> putProcedure(
            @PathVariable Long id,
            @RequestParam("title") String title,
            @RequestParam("subtitle") String subtitle,
            @RequestParam("steps") String steps,
            @RequestParam("questions") String questions,
            @RequestParam(value = "picture", required = false) String picture
    ) {
        try {
            log.info("Updating procedure ...");

            Procedure procedure = new Procedure();
            procedure.setId(id);
            procedure.setTitle(title);
            procedure.setSubtitle(subtitle);
            procedure.setSteps(steps);
            procedure.setQuestions(questions);
            procedure.setPicture(picture);

            Procedure saved = this.procedureService.updateProcedure(procedure);
            return new ResponseEntity<>(saved, HttpStatus.ACCEPTED);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Procedure> deleteProcedure(@PathVariable Long id) {
        try {
            log.info("Deleting procedure ...");
            return new ResponseEntity<>(this.procedureService.deleteProcedure(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}