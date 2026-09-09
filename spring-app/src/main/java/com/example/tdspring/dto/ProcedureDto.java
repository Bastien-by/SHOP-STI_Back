package com.example.tdspring.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProcedureDto {
    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private List<String> steps;
    private List<String> questions;
    private LocalDateTime createdAt;
}