package com.example.tdspring.services;

import com.example.tdspring.dto.ProcedureDto;
import com.example.tdspring.models.Procedure;
import com.example.tdspring.repositories.ProcedureRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<ProcedureDto> getAll() {
        return procedureRepository.findAllByOrderByCreatedAtAsc()
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    public ProcedureDto create(ProcedureDto dto) {
        return toDto(procedureRepository.save(toEntity(dto)));
    }

    public ProcedureDto update(Long id, ProcedureDto dto) {
        Procedure proc = procedureRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Procédure introuvable : " + id));
        proc.setTitle(dto.getTitle());
        proc.setSubtitle(dto.getSubtitle());
        proc.setDescription(dto.getDescription());
        proc.setSteps(toJson(dto.getSteps()));
        proc.setQuestions(toJson(dto.getQuestions()));
        proc.setWarning(dto.getWarning());
        proc.setImagePath(dto.getImagePath());
        proc.setImageAlt(dto.getImageAlt());
        proc.setCategory(dto.getCategory());
        return toDto(procedureRepository.save(proc));
    }

    public void delete(Long id) {
        procedureRepository.deleteById(id);
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private ProcedureDto toDto(Procedure proc) {
        ProcedureDto dto = new ProcedureDto();
        dto.setId(proc.getId());
        dto.setTitle(proc.getTitle());
        dto.setSubtitle(proc.getSubtitle());
        dto.setDescription(proc.getDescription());
        dto.setSteps(fromJson(proc.getSteps()));
        dto.setQuestions(fromJson(proc.getQuestions()));
        dto.setWarning(proc.getWarning());
        dto.setImagePath(proc.getImagePath());
        dto.setImageAlt(proc.getImageAlt());
        dto.setCategory(proc.getCategory());
        dto.setCreatedAt(proc.getCreatedAt());
        dto.setCreatedBy(proc.getCreatedBy());
        return dto;
    }

    private Procedure toEntity(ProcedureDto dto) {
        Procedure proc = new Procedure();
        proc.setTitle(dto.getTitle());
        proc.setSubtitle(dto.getSubtitle());
        proc.setDescription(dto.getDescription());
        proc.setSteps(toJson(dto.getSteps()));
        proc.setQuestions(toJson(dto.getQuestions()));
        proc.setWarning(dto.getWarning());
        proc.setImagePath(dto.getImagePath());
        proc.setImageAlt(dto.getImageAlt());
        proc.setCategory(dto.getCategory());
        proc.setCreatedBy(dto.getCreatedBy());
        return proc;
    }

    private String toJson(List<String> list) {
        if (list == null) return "[]";
        try { return objectMapper.writeValueAsString(list); }
        catch (Exception e) { return "[]"; }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try { return objectMapper.readValue(json, new TypeReference<>() {}); }
        catch (Exception e) { return List.of(); }
    }
}