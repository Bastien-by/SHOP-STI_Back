package com.example.tdspring.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;

import java.util.Date;

@Entity(name = "stock")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class Stock {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_product")
    private Product product;

    private Boolean available;

    private Integer status; // 0: NOK, 1: OK, 2: HS

    private Date creationDate;

    private String alitracer;

    private String reference;

    @Column(nullable = true)
    private Integer lockerNumber;

    @Column(nullable = true)
    private String emplacement;
}