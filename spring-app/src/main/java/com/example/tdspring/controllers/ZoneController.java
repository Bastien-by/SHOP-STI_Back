package com.example.tdspring.controllers;

import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Zone;
import com.example.tdspring.services.ZoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/zones")
@RequiredArgsConstructor
@Slf4j
public class ZoneController {

    private final ZoneService zoneService;

    @GetMapping
    public ResponseEntity<List<Zone>> getZones() {
        return new ResponseEntity<>(zoneService.getAllZones(), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Zone> getZone(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(zoneService.getZone(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    public ResponseEntity<Zone> saveZone(@RequestBody Zone zone) {
        Zone saved = zoneService.createOrUpdate(zone);
        HttpStatus status = (zone.getId() == null) ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return new ResponseEntity<>(saved, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteZone(@PathVariable Long id) {
        try {
            zoneService.deleteZone(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}