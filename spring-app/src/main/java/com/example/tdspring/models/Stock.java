package com.example.tdspring.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;

import java.util.Date;

@Entity
@Table(name = "stock")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Produit lié
    @ManyToOne
    @JoinColumn(name = "id_product")
    private Product product;

    // Disponible ou non
    private Boolean available;

    // 0: NOK, 1: OK, 2: HS
    private Integer status;

    private Date creationDate;

    private String alitracer;

    private String reference;

    @Column(nullable = true)
    private Integer lockerNumber;

    @Column(nullable = true)
    private String emplacement;

    // Zone atelier
    @ManyToOne
    @JoinColumn(name = "zone_id")  // colonne zone_id dans la table stock
    private Zone zone;
}