package com.example.tdspring.services;

import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Zone;
import com.example.tdspring.repositories.ZoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZoneService {

    private final ZoneRepository zoneRepository;

    public List<Zone> getAllZones() {
        return zoneRepository.findAll();
    }

    public Zone getZone(Long id) throws NotFoundException {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Zone not found: " + id));
    }

    public Zone createOrUpdate(Zone zone) {
        return zoneRepository.save(zone);
    }

    public void deleteZone(Long id) throws NotFoundException {
        Zone z = getZone(id);
        zoneRepository.delete(z);
    }
}