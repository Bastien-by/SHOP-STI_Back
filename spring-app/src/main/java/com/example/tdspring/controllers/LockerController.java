package com.example.tdspring.controllers;

import com.example.tdspring.services.SiemensPlcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locker")
@CrossOrigin(origins = "*")
public class LockerController {

    private static final Logger log = LoggerFactory.getLogger(LockerController.class);

    @Autowired
    private SiemensPlcService siemensPlcService;

    /* =========================================================
     *  COMMANDES INDIVIDUELLES
     * ========================================================= */

    @PostMapping("/open/{lockerId}")
    public ResponseEntity<Map<String, Object>> openLocker(@PathVariable int lockerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = siemensPlcService.openLocker(lockerId);
            response.put("success", success);
            response.put("lockerId", lockerId);
            response.put("message", success
                    ? "Casier " + lockerId + " ouvert"
                    : "Échec ouverture casier");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur ouverture casier {}", lockerId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/close/{lockerId}")
    public ResponseEntity<Map<String, Object>> closeLocker(@PathVariable int lockerId) {
        Map<String, Object> response = new HashMap<>();
        try {
            boolean success = siemensPlcService.closeLocker(lockerId);
            response.put("success", success);
            response.put("lockerId", lockerId);
            response.put("message", success
                    ? "Casier " + lockerId + " fermé"
                    : "Échec fermeture casier");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Erreur fermeture casier {}", lockerId, e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /* =========================================================
     *  COMMANDES BATCH (plusieurs casiers, 1 seule requête JSON-RPC)
     *
     *  POST /locker/openLockers   body: { "lockerIds": [1, 3, 7] }
     *  POST /locker/closeLockers  body: { "lockerIds": [1, 3, 7] }
     *
     *  Réponse : { "requested": [...], "succeeded": [...], "failed": [...] }
     *  HTTP 200 si tout a réussi, 206 Partial Content si certains ont échoué.
     * ========================================================= */

    /**
     * Ouvre une liste de casiers en une seule requête JSON-RPC batch.
     * Appelé par le contrôle réglementaire global depuis le frontend.
     */
    @PostMapping("/openLockers")
    public ResponseEntity<Map<String, Object>> openLockers(
            @RequestBody Map<String, List<Integer>> body) {

        List<Integer> requested = body.get("lockerIds");
        if (requested == null || requested.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("📦 BATCH OPEN casiers {}", requested);
        Map<String, Object> result = new HashMap<>();

        try {
            List<Integer> succeeded = siemensPlcService.openLockers(requested);
            List<Integer> failed    = requested.stream()
                    .filter(id -> !succeeded.contains(id))
                    .collect(Collectors.toList());

            result.put("requested",  requested);
            result.put("succeeded",  succeeded);
            result.put("failed",     failed);

            log.info("✅ Batch open — réussis: {} / échoués: {}", succeeded, failed);

            return failed.isEmpty()
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);

        } catch (Exception e) {
            log.error("❌ Erreur batch open casiers {}", requested, e);
            result.put("requested", requested);
            result.put("succeeded", List.of());
            result.put("failed",    requested);
            result.put("error",     e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Ferme une liste de casiers en une seule requête JSON-RPC batch.
     * Appelé après confirmation de fermeture depuis le frontend.
     */
    @PostMapping("/closeLockers")
    public ResponseEntity<Map<String, Object>> closeLockers(
            @RequestBody Map<String, List<Integer>> body) {

        List<Integer> requested = body.get("lockerIds");
        if (requested == null || requested.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("📦 BATCH CLOSE casiers {}", requested);
        Map<String, Object> result = new HashMap<>();

        try {
            List<Integer> succeeded = siemensPlcService.closeLockers(requested);
            List<Integer> failed    = requested.stream()
                    .filter(id -> !succeeded.contains(id))
                    .collect(Collectors.toList());

            result.put("requested",  requested);
            result.put("succeeded",  succeeded);
            result.put("failed",     failed);

            log.info("✅ Batch close — réussis: {} / échoués: {}", succeeded, failed);

            return failed.isEmpty()
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(result);

        } catch (Exception e) {
            log.error("❌ Erreur batch close casiers {}", requested, e);
            result.put("requested", requested);
            result.put("succeeded", List.of());
            result.put("failed",    requested);
            result.put("error",     e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /* =========================================================
     *  MANAGE ALL (ouvrir / fermer les 24 casiers d'un coup)
     * ========================================================= */

    @PostMapping("/all/open")
    public ResponseEntity<Map<String, Object>> openAllLockers() {
        log.info("📦 Ouverture de tous les casiers via Manage_All");
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = siemensPlcService.writeManageAll(true);
            result.put("successCount", success ? 24 : 0);
            result.put("failCount",    success ? 0 : 24);
            result.put("total",        24);
            result.put("message", success
                    ? "Commande d'ouverture de tous les casiers envoyée"
                    : "Échec de la commande d'ouverture");
            return success
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } catch (Exception e) {
            log.error("❌ Erreur ouverture tous casiers", e);
            result.put("successCount", 0);
            result.put("failCount", 24);
            result.put("total", 24);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/all/close")
    public ResponseEntity<Map<String, Object>> closeAllLockers() {
        log.info("📦 Fermeture de tous les casiers via Manage_All");
        Map<String, Object> result = new HashMap<>();
        try {
            boolean success = siemensPlcService.writeManageAll(false);
            result.put("successCount", success ? 24 : 0);
            result.put("failCount",    success ? 0 : 24);
            result.put("total",        24);
            result.put("message", success
                    ? "Commande de fermeture de tous les casiers envoyée"
                    : "Échec de la commande de fermeture");
            return success
                    ? ResponseEntity.ok(result)
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } catch (Exception e) {
            log.error("❌ Erreur fermeture tous casiers", e);
            result.put("successCount", 0);
            result.put("failCount", 24);
            result.put("total", 24);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}