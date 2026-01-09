package com.example.tdspring.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pdf")
@CrossOrigin(origins = "*")
public class PdfController {

    // Dossier où sauvegarder les PDFs
    private static final String PDF_DIRECTORY = "/var/www/OPtiBox/SHOP-STI_Back/pdfs/";

    @PostMapping("/save")
    public ResponseEntity<Map<String, String>> savePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("stockId") String stockId,
            @RequestParam("checkDate") String checkDate,
            @RequestParam("alitracer") String alitracer) {

        try {
            File directory = new File(PDF_DIRECTORY);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String filename = file.getOriginalFilename();
            Path filepath = Paths.get(PDF_DIRECTORY, filename);
            Files.copy(file.getInputStream(), filepath, StandardCopyOption.REPLACE_EXISTING);

            System.out.println("✅ PDF sauvegardé : " + filepath.toString());

            // ✅ RETOURNE DU JSON
            Map<String, String> response = new HashMap<>();
            response.put("message", "PDF sauvegardé avec succès");
            response.put("filepath", filepath.toString());
            response.put("filename", filename);

            return ResponseEntity.ok(response);  // ✅ Retourne du JSON

        } catch (IOException e) {
            e.printStackTrace();

            Map<String, String> error = new HashMap<>();
            error.put("error", "Erreur lors de la sauvegarde du PDF");

            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/download/{filename}")
    @Operation(summary = "Télécharge un PDF par son nom de fichier")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String filename) {
        try {
            Path filepath = Paths.get(PDF_DIRECTORY, filename);
            Resource resource = new UrlResource(filepath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}
