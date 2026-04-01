package com.example.tdspring.services;

import com.example.tdspring.exceptions.DBException;
import com.example.tdspring.exceptions.NotFoundException;
import com.example.tdspring.models.Check;
import com.example.tdspring.repositories.CheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckService {

    private final CheckRepository checkRepository;

    public List<Check> getAllChecks() {
        return this.checkRepository.findAll();
    }

    /**
     * Crée ou met à jour un check.
     *
     * Champs persistés :
     *   - stock, date, user, status, comment  (existants)
     *   - pdfFilename                          (était oublié — corrigé)
     *   - checkType                            (nouveau : "REGULATORY" | "INDIVIDUAL" | null)
     */
    public Check updateCheck(Check check) throws DBException, NotFoundException {
        Check existing;

        if (check.getId() != null) {
            existing = this.checkRepository.findById(check.getId())
                    .orElseThrow(() -> new NotFoundException(
                            "Could not find check with id : " + check.getId()));
        } else {
            existing = new Check();
        }

        existing.setStock(check.getStock());
        existing.setDate(check.getDate());
        existing.setComment(check.getComment());
        existing.setStatus(check.getStatus());
        existing.setUser(check.getUser());

        // ✅ pdfFilename : ne pas écraser une valeur existante si le nouveau est null
        if (check.getPdfFilename() != null) {
            existing.setPdfFilename(check.getPdfFilename());
        }

        // ✅ checkType : "REGULATORY" | "INDIVIDUAL" | null (rétrocompat)
        existing.setCheckType(check.getCheckType());

        try {
            return this.checkRepository.save(existing);
        } catch (Exception e) {
            throw new DBException("Could not save check");
        }
    }

    public Check deleteCheck(Long id) throws NotFoundException, DBException {
        Check existing = this.checkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Could not find check with id : " + id));
        try {
            this.checkRepository.delete(existing);
            return existing;
        } catch (Exception e) {
            throw new DBException("Could not delete check");
        }
    }

    public Check getCheckById(Long id) throws NotFoundException {
        return checkRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Check not found with id: " + id));
    }

    /**
     * Retourne TOUS les checks d'un stock avec toutes leurs données
     * (date, checkType, status, comment…).
     *
     * ⚠️ Remplace l'ancienne méthode qui ne renvoyait qu'un Integer (le count).
     *    Le frontend a besoin de la liste complète pour calculer :
     *      - lastCheckDate           (tous types → affichage dialog)
     *      - lastRegulatoryCheckDate (REGULATORY only → ratio + couleur + Excel)
     */
    public List<Check> getChecksByStockId(Long stockId) {
        return this.checkRepository.findByStockId(stockId);
    }
}