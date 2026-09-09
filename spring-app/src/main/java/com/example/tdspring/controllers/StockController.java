package com.example.tdspring.controllers;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Stock;
import com.example.tdspring.services.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stocks")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockService stockService;

    @GetMapping
    public ResponseEntity<List<Stock>> getStocks() {
        return new ResponseEntity<>(this.stockService.getAllStocks(), HttpStatus.OK);
    }

    @GetMapping("/getAvailableStocks")
    public ResponseEntity<List<Stock>> getAvalaibleStocks() {
        return new ResponseEntity<>(this.stockService.getAvalaibleStocks(), HttpStatus.OK);
    }

    @GetMapping("/getUnavailableStocks")
    public ResponseEntity<List<Stock>> getUnavailableStocks() {
        return new ResponseEntity<>(this.stockService.getUnavailableStocks(), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<Stock> postStock(@RequestBody Stock stockSent) {
        try {
            System.out.println("############ BACK RECOIT STOCK, zone = "
                    + (stockSent.getZone() != null ? stockSent.getZone().getId() : "NULL"));
            log.info(">>> DEBUG STOCK POST <<< payload zone_id = {}",
                    stockSent.getZone() != null ? stockSent.getZone().getId() : null);

            Stock saved = this.stockService.updateStock(stockSent);

            HttpStatus status = stockSent.getId() == null ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
            return new ResponseEntity<>(saved, status);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/{id}/remove-from-locker")
    public ResponseEntity<Stock> removeFromLocker(@PathVariable Long id) {
        try {
            log.info("Removing stock {} from locker ...", id);
            return new ResponseEntity<>(this.stockService.removeFromLocker(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Stock> deleteStock(@PathVariable Long id) {
        try {
            log.info("Deleting stock {} ...", id);
            return new ResponseEntity<>(this.stockService.deleteStock(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (DBException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Stock> getStock(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(this.stockService.getStock(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/getStockByProductId/{id}")
    public ResponseEntity<Integer> getStockByProductId(@PathVariable Long id) {
        try {
            return new ResponseEntity<>(this.stockService.getStockByProductId(id), HttpStatus.OK);
        } catch (NotFoundException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}