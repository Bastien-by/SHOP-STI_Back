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
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.util.List;

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

            // Mettre à jour le statut du stock
            Stock stock = checkSent.getStock();
            stock.setStatus(checkSent.getStatus());
            stockService.updateStock(stock);

            // Enregistrer le check (création ou mise à jour)
            Check savedCheck = this.checkService.updateCheck(checkSent);

            // Retourner le bon code HTTP selon si c'est une création ou une mise à jour
            HttpStatus responseStatus = (checkSent.getId() == null) ? HttpStatus.CREATED : HttpStatus.ACCEPTED;

            return new ResponseEntity<>(savedCheck, responseStatus);

        } catch (DBException e) {
            log.error("Database error while saving check: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFoundException e) {
            log.error("Check creation failed: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Check> deleteCheck(@PathVariable Long id) {
        try {
            log.info("Deleting check ...");
            return new ResponseEntity<>(this.checkService.deleteCheck(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/getCheckByStockId/{id}")
    public ResponseEntity<Integer> getCheckByStockId(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(this.checkService.getCheckByStockId(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

}
