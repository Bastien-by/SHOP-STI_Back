package com.example.tdspring.controllers;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Check;
import com.example.tdspring.models.Stock;
import com.example.tdspring.services.CheckService;
import com.example.tdspring.services.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/checks")
@RequiredArgsConstructor
@Slf4j
public class CheckController {

    private final CheckService checkService;
    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<Check>> getChecks() {
        return new ResponseEntity<>(this.checkService.getAllChecks(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Check> postCheck(@RequestBody Check checkSent) {
        try {
            log.info("Creating check: {}", checkSent);

            if (checkSent.getStock() == null || checkSent.getStock().getId() == null) {
                throw new NotFoundException("Stock not found");
            }

            // Met à jour le statut du stock
            Stock stock = checkSent.getStock();
            stock.setStatus(checkSent.getStatus());
            stockService.updateStock(stock);

            // Enregistre le check (checkType + pdfFilename inclus)
            Check savedCheck = this.checkService.updateCheck(checkSent);

            HttpStatus responseStatus = (checkSent.getId() == null)
                    ? HttpStatus.CREATED
                    : HttpStatus.ACCEPTED;

            return new ResponseEntity<>(savedCheck, responseStatus);

        } catch (DBException e) {
            log.error("Database error while saving check: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFoundException e) {
            log.error("Check creation failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * Met à jour le filename PDF d'un check existant.
     * Appelé par le frontend après la génération du PDF.
     */
    @PatchMapping("/{checkId}/pdf")
    public ResponseEntity<Check> updatePdfFilename(
            @PathVariable Long checkId,
            @RequestParam String filename) {
        try {
            log.info("Updating PDF filename for check {}: {}", checkId, filename);

            Check check = this.checkService.getCheckById(checkId);
            check.setPdfFilename(filename);
            Check updatedCheck = this.checkService.updateCheck(check);

            return new ResponseEntity<>(updatedCheck, HttpStatus.OK);

        } catch (NotFoundException e) {
            log.error("Check not found: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (DBException e) {
            log.error("Database error while updating check: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Check> deleteCheck(@PathVariable Long id) {
        try {
            log.info("Deleting check {}", id);
            return new ResponseEntity<>(this.checkService.deleteCheck(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retourne la LISTE COMPLÈTE des checks d'un stock.
     *
     * ⚠️ BREAKING CHANGE : anciennement retournait Integer (count).
     *    Désormais retourne List<Check> avec date, checkType, status, comment.
     *
     * Le frontend l'utilise pour calculer :
     *   - lastCheckDate           (tous types → affichage)
     *   - lastRegulatoryCheckDate (checkType = 'REGULATORY' → ratio + couleur + Excel)
     *
     * URL : GET /checks/getCheckByStockId/{id}
     */
    @GetMapping("/getCheckByStockId/{id}")
    public ResponseEntity<List<Check>> getCheckByStockId(@PathVariable Long id) {
        try {
            List<Check> checks = this.checkService.getChecksByStockId(id);
            return new ResponseEntity<>(checks, HttpStatus.OK);
        } catch (Exception e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Retourne TOUS les checks groupés par stock ID.
     * Remplace les N appels individuels /getCheckByStockId/{id}
     * par une seule requête au démarrage.
     */
    @GetMapping("/all-by-stocks")
    public ResponseEntity<Map<Long, List<Check>>> getAllChecksByStocks() {
        List<Check> all = this.checkService.getAllChecks();

        Map<Long, List<Check>> grouped = all.stream()
                .filter(c -> c.getStock() != null)
                .collect(Collectors.groupingBy(c -> c.getStock().getId()));

        return new ResponseEntity<>(grouped, HttpStatus.OK);
    }
}