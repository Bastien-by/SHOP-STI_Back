package com.example.tdspring.services;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Stock;
import com.example.tdspring.models.Zone;
import com.example.tdspring.repositories.StockRepository;
import com.example.tdspring.repositories.ZoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockService {

    private final StockRepository stockRepository;
    private final ZoneRepository zoneRepository;

    public List<Stock> getAllStocks() {
        return this.stockRepository.findAll();
    }

    public List<Stock> getAvalaibleStocks() {
        return this.stockRepository.findByAvailable(true);
    }

    public List<Stock> getUnavailableStocks() {
        return this.stockRepository.findByAvailable(false);
    }

    public Stock updateStock(Stock stock) throws DBException, NotFoundException {
        Stock existing;

        // Création ou update selon présence d'un id
        if (stock.getId() != null) {
            existing = stockRepository.findById(stock.getId())
                    .orElseThrow(() -> new NotFoundException("Could not find stock with id : " + stock.getId()));
        } else {
            existing = new Stock();
        }

        // Champs simples : mise à jour seulement si non null dans la requête
        if (stock.getProduct() != null)      existing.setProduct(stock.getProduct());
        if (stock.getAlitracer() != null)    existing.setAlitracer(stock.getAlitracer());
        if (stock.getReference() != null)    existing.setReference(stock.getReference());
        if (stock.getAvailable() != null)    existing.setAvailable(stock.getAvailable());
        if (stock.getStatus() != null)       existing.setStatus(stock.getStatus());
        if (stock.getCreationDate() != null) existing.setCreationDate(stock.getCreationDate());
        if (stock.getEmplacement() != null)  existing.setEmplacement(stock.getEmplacement());
        if (stock.getLockerNumber() != null) existing.setLockerNumber(stock.getLockerNumber());

        // Zone : on la gère TOUJOURS explicitement
        if (stock.getZone() == null) {
            // le client a envoyé "zone": null -> on enlève la zone
            existing.setZone(null);
            log.debug("Stock {} : zone mise à null", existing.getId());
        } else if (stock.getZone().getId() != null) {
            // le client a envoyé "zone": { "id": X }
            Long zoneId = stock.getZone().getId();
            Zone z = zoneRepository.findById(zoneId)
                    .orElseThrow(() -> new NotFoundException("Zone not found: " + zoneId));
            existing.setZone(z);
            log.debug("Stock {} : zone mise à {}", existing.getId(), zoneId);
        } else {
            // zone envoyée sans id -> on la considère comme null
            existing.setZone(null);
            log.debug("Stock {} : zone sans id -> null", existing.getId());
        }

        try {
            Stock saved = stockRepository.save(existing);
            log.info("Saved stock {}, zone_id={}", saved.getId(),
                    saved.getZone() != null ? saved.getZone().getId() : null);
            return saved;
        } catch (Exception e) {
            throw new DBException("Could not create stock");
        }
    }

    public Stock removeFromLocker(Long id) throws NotFoundException, DBException {
        Stock existing = this.stockRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new NotFoundException("Could not find stock with id : " + id);
        }
        existing.setLockerNumber(null);
        existing.setEmplacement(null);
        try {
            return this.stockRepository.save(existing);
        } catch (Exception e) {
            throw new DBException("Could not update stock");
        }
    }

    public Stock deleteStock(Long id) throws NotFoundException, DBException {
        Stock existing = this.stockRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new NotFoundException("Could not find stock with id : " + id);
        }
        try {
            this.stockRepository.delete(existing);
            return existing;
        } catch (Exception e) {
            throw new DBException("Could not delete stock");
        }
    }

    public Stock getStock(Long id) throws NotFoundException {
        Stock existing = this.stockRepository.findById(id).orElse(null);
        if (existing == null) {
            throw new NotFoundException("Could not find stock with id : " + id);
        }
        return existing;
    }

    public Integer getStockByProductId(Long productId) throws NotFoundException {
        List<Stock> stocks = this.stockRepository.findByProductId(productId);
        return stocks.size();
    }
}