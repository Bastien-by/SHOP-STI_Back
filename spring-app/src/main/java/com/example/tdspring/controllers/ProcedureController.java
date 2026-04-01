package com.example.tdspring.controllers;

import com.example.tdspring.dto.ProcedureDto;
import com.example.tdspring.services.ProcedureService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/procedures")
@RequiredArgsConstructor
public class ProcedureController {

    private final ProcedureService procedureService;
    private static final String IMAGE_DIR = "uploads/procedures/";

    @GetMapping
    public ResponseEntity<List<ProcedureDto>> getAll() {
        return ResponseEntity.ok(procedureService.getAll());
    }

    @PostMapping
    public ResponseEntity<ProcedureDto> create(@RequestBody ProcedureDto dto) {
        return ResponseEntity.ok(procedureService.create(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcedureDto> update(
            @PathVariable Long id,
            @RequestBody ProcedureDto dto) {
        return ResponseEntity.ok(procedureService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        procedureService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path dest = Paths.get(IMAGE_DIR + filename);
        Files.createDirectories(dest.getParent());
        Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        return ResponseEntity.ok("uploads/procedures/" + filename);
    }
}