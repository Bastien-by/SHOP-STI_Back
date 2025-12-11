package com.example.tdspring.controllers;

import com.example.tdspring.services.SiemensPlcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/locker")
@CrossOrigin(origins = "*") // Pour les tests Postman
public class LockerController {

    @Autowired
    private SiemensPlcService siemensPlcService;

    // Test 1: Ping l'automate
    @PostMapping("/ping")
    public ResponseEntity<Map<String, Object>> testPing() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = siemensPlcService.ping();
            response.put("success", success);
            response.put("message", success ? "PLC joignable" : "PLC non joignable");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Test 2: Login au PLC
    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> testLogin() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = siemensPlcService.login();
            response.put("success", success);
            response.put("message", success ? "Login réussi" : "Login échoué");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // Test 3: Ouvrir un casier
    @PostMapping("/open/{lockerId}")
    public ResponseEntity<Map<String, Object>> openLocker(@PathVariable int lockerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = siemensPlcService.openLocker(lockerId);
            response.put("success", success);
            response.put("lockerId", lockerId);
            response.put("message", success ? "Casier " + lockerId + " ouvert" : "Échec ouverture casier");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
