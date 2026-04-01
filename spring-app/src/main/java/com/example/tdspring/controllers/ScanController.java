package com.example.tdspring.controllers;

import com.example.tdspring.services.SiemensPlcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/scan")
@CrossOrigin(origins = "*")
public class ScanController {

    @Autowired
    private SiemensPlcService siemensPlcService;

    /**
     * Endpoint pour lire la valeur scannée par la douchette depuis l'automate
     * POST /scan/read
     *
     * Retourne :
     * {
     *   "success": true,
     *   "value": "100902",
     *   "timestamp": 1738053600000
     * }
     * ou
     * {
     *   "success": false,
     *   "value": "",
     *   "message": "Aucune valeur scannée"
     * }
     */
    @PostMapping("/read")
    public ResponseEntity<Map<String, Object>> readScan() {
        Map<String, Object> response = new HashMap<>();
        try {
            String scanValue = siemensPlcService.readScan();

            if (scanValue != null && !scanValue.isEmpty()) {
                response.put("success", true);
                response.put("value", scanValue);
                response.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("value", "");
                response.put("message", "Aucune valeur scannée");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("value", "");
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint pour effacer la valeur scannée dans l'automate après traitement
     * POST /scan/clear
     *
     * Évite de relire la même valeur au prochain polling
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearScan() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = siemensPlcService.clearScan();
            response.put("success", success);
            response.put("message", success ? "Scan cleared" : "Failed to clear scan");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Endpoint de test pour vérifier la connectivité PLC
     * GET /scan/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean plcOk = siemensPlcService.healthCheck();
            response.put("status", plcOk ? "ok" : "error");
            response.put("plcConnected", plcOk);
            response.put("message", plcOk ? "PLC communication operational" : "PLC communication failed");

            if (plcOk) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("plcConnected", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        }
    }
}
