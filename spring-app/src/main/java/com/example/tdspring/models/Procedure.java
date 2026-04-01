package com.example.tdspring.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Data;
import java.time.LocalDateTime;

@Entity(name = "procedure")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Data
public class Procedure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String steps;       // JSON : ["étape 1", "étape 2"]

    @Column(columnDefinition = "TEXT")
    private String questions;   // JSON : ["question 1", "question 2"]

    @Column(columnDefinition = "TEXT")
    private String warning;

    private String imagePath;
    private String imageAlt;
    private String category;

    private LocalDateTime createdAt = LocalDateTime.now();
    private String createdBy;
}