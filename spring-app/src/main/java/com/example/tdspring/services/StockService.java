package com.example.tdspring.services;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Stock;
import com.example.tdspring.repositories.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;

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

        if (stock.getId() != null) {
            existing = this.stockRepository.findById(stock.getId()).orElse(null);
            if (existing == null) throw new NotFoundException("Could not find stock with id : " + stock.getId());
        } else {
            existing = new Stock();
        }

        if (stock.getProduct() != null)       existing.setProduct(stock.getProduct());
        if (stock.getAvailable() != null)     existing.setAvailable(stock.getAvailable());
        if (stock.getStatus() != null)        existing.setStatus(stock.getStatus());
        if (stock.getCreationDate() != null)  existing.setCreationDate(stock.getCreationDate());
        if (stock.getReference() != null)     existing.setReference(stock.getReference());
        if (stock.getAlitracer() != null)     existing.setAlitracer(stock.getAlitracer());

        // lockerNumber : on autorise explicitement la mise à null via un patch séparé
        // ici on ne met à jour que si la valeur est fournie (non null)
        if (stock.getLockerNumber() != null)  existing.setLockerNumber(stock.getLockerNumber());

        // emplacement : même logique, on met à jour si fourni
        if (stock.getEmplacement() != null)   existing.setEmplacement(stock.getEmplacement());

        try {
            return this.stockRepository.save(existing);
        } catch (Exception e) {
            throw new DBException("Could not create stock");
        }
    }

    /**
     * Met à null le lockerNumber ET l'emplacement d'un stock (retrait physique du casier)
     */
    public Stock removeFromLocker(Long id) throws NotFoundException, DBException {
        Stock existing = this.stockRepository.findById(id).orElse(null);
        if (existing == null) throw new NotFoundException("Could not find stock with id : " + id);
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
        if (existing == null) throw new NotFoundException("Could not find stock with id : " + id);
        try {
            this.stockRepository.delete(existing);
            return existing;
        } catch (Exception e) {
            throw new DBException("Could not delete stock");
        }
    }

    public Stock getStock(Long id) throws NotFoundException {
        Stock existing = this.stockRepository.findById(id).orElse(null);
        if (existing == null) throw new NotFoundException("Could not find stock with id : " + id);
        return existing;
    }

    public Integer getStockByProductId(Long productId) throws NotFoundException {
        List<Stock> stocks = this.stockRepository.findByProductId(productId);
        return stocks.size();
    }
}