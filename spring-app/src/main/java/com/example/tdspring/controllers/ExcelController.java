package com.example.tdspring.controllers;

import com.example.tdspring.dto.CheckRecordDTO;
import com.example.tdspring.dto.MonthlyExcelFileDTO;
import com.example.tdspring.services.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @GetMapping("/monthly-files")
    public ResponseEntity<List<MonthlyExcelFileDTO>> getMonthlyFiles() {
        return ResponseEntity.ok(excelService.getMonthlyFiles());
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadMonthlyExcel(@PathVariable String filename) {
        try {
            Path filePath = excelService.getMonthlyFilePath(filename);
            Resource resource = new UrlResource(filePath.toUri());

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/update-monthly")
    public ResponseEntity<Void> updateMonthlyExcel(@RequestBody CheckRecordDTO checkRecord) {
        excelService.updateMonthlyExcel(checkRecord);
        return ResponseEntity.ok().build();
    }
}
