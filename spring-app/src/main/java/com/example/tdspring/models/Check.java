package com.example.tdspring.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;
import java.util.Date;

@Entity(name = "check")
@Table(name = "checks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class Check implements Action {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "stock_id")
    private Stock stock;

    private Date date;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String pdfFilename;

    private Integer status; // 1 = OK, 0 = NOK, 2 = HS

    private String comment;

    /**
     * Type du contrôle :
     *   "REGULATORY"  → contrôle réglementaire mensuel
     *                   → met à jour le ratio casier + fichier Excel
     *   "INDIVIDUAL"  → contrôle individuel libre
     *                   → affiché dans le dialog uniquement, sans impact ratio/Excel
     *   null          → ancien check (rétrocompatibilité) → traité comme REGULATORY côté front
     */
    @Column(name = "check_type", nullable = true)
    private String checkType;

    @Override
    public String getType() {
        return "check";
    }
}