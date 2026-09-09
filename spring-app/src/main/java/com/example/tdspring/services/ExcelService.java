package com.example.tdspring.services;

import com.example.tdspring.dto.CheckRecordDTO;
import com.example.tdspring.dto.MonthlyExcelFileDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ExcelService {

    private static final String EXCEL_DIR = "data/monthly-controls/";
    private static final String TEMPLATE_PATH = "templates/Controle_Template.xlsx";
    private static final TimeZone TZ_PARIS = TimeZone.getTimeZone("Europe/Paris");

    @PostConstruct
    public void init() {
        new File(EXCEL_DIR).mkdirs();
    }

    public List<MonthlyExcelFileDTO> getMonthlyFiles() {
        File dir = new File(EXCEL_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".xlsx"));

        if (files == null) return Collections.emptyList();

        return Arrays.stream(files)
                .map(file -> {
                    MonthlyExcelFileDTO dto = new MonthlyExcelFileDTO();
                    dto.setFilename(file.getName());
                    dto.setDisplayName(file.getName().replace(".xlsx", "").replace("_", " "));
                    dto.setLastUpdate(new Date(file.lastModified()));
                    dto.setRecordCount(getRecordCount(file));
                    return dto;
                })
                .sorted(Comparator.comparing(MonthlyExcelFileDTO::getLastUpdate).reversed())
                .collect(Collectors.toList());
    }

    private int getRecordCount(File file) {
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
            Sheet sheet = workbook.getSheetAt(0);
            return sheet.getLastRowNum();
        } catch (Exception e) {
            return 0;
        }
    }

    public Path getMonthlyFilePath(String filename) {
        return Paths.get(EXCEL_DIR + filename);
    }

    public void updateMonthlyExcel(CheckRecordDTO checkRecord) {
        String filename = generateFilename(checkRecord.getDate());
        Path filePath = Paths.get(EXCEL_DIR + filename);

        try {
            if (!Files.exists(filePath)) {
                createFromTemplate(filePath);
            }

            try (FileInputStream fis = new FileInputStream(filePath.toFile());
                 Workbook workbook = new XSSFWorkbook(fis)) {

                Sheet sheet = workbook.getSheetAt(0);

                int rowIndex = findRowByAlitracer(sheet, checkRecord.getAlitracer());

                Row row;
                if (rowIndex == -1) {
                    rowIndex = sheet.getLastRowNum() + 1;
                    row = sheet.createRow(rowIndex);
                } else {
                    row = sheet.getRow(rowIndex);
                }

                // Formatter d'affichage avec timezone Paris
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                displayFormat.setTimeZone(TZ_PARIS);

                row.createCell(0).setCellValue(checkRecord.getAlitracer());
                row.createCell(1).setCellValue(checkRecord.getSize());
                row.createCell(2).setCellValue(checkRecord.getCmu());
                row.createCell(3).setCellValue(checkRecord.getLocation());
                row.createCell(4).setCellValue(getStatusLabel(checkRecord.getStatus()));
                row.createCell(5).setCellValue(checkRecord.getComment());
                row.createCell(6).setCellValue(checkRecord.getControlledBy());
                row.createCell(7).setCellValue(displayFormat.format(checkRecord.getDate()));

                try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                    workbook.write(fos);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur mise à jour Excel: " + e.getMessage(), e);
        }
    }

    private int findRowByAlitracer(Sheet sheet, String alitracer) {
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row != null && row.getCell(0) != null) {
                if (alitracer.equals(row.getCell(0).getStringCellValue())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void createFromTemplate(Path targetPath) throws IOException {
        InputStream templateStream = getClass().getClassLoader().getResourceAsStream(TEMPLATE_PATH);
        if (templateStream == null) {
            createEmptyWorkbook(targetPath);
        } else {
            Files.copy(templateStream, targetPath);
        }
    }

    private void createEmptyWorkbook(Path targetPath) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Contrôles");

            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID Alitracer", "Taille", "CMU", "Emplacement", "Statut", "Commentaire", "Contrôleur", "Date"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                CellStyle style = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                style.setFont(font);
                cell.setCellStyle(style);
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                workbook.write(fos);
            }
        }
    }

    private String generateFilename(Date date) {
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM", Locale.FRENCH);
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");

        // Timezone Paris sur les deux formatters pour éviter le décalage UTC
        monthFormat.setTimeZone(TZ_PARIS);
        yearFormat.setTimeZone(TZ_PARIS);

        String month = capitalize(monthFormat.format(date));
        String year = yearFormat.format(date);

        return String.format("Controle_%s_%s.xlsx", month, year);
    }

    private String capitalize(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private String getStatusLabel(int status) {
        switch (status) {
            case 1: return "OK";
            case 0: return "NOK";
            case 2: return "HS";
            default: return "Inconnu";
        }
    }
}