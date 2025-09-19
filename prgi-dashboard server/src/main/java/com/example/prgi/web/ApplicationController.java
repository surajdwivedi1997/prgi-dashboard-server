package com.example.prgi.web;

import com.example.prgi.domain.ModuleType;
import com.example.prgi.domain.Status;
import com.example.prgi.repo.ApplicationRepository;
import com.example.prgi.web.dto.ApplicationDto;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")
@CrossOrigin(origins = "*")
public class ApplicationController {
    private final ApplicationRepository repo;

    public ApplicationController(ApplicationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ApplicationDto> list(
            @RequestParam ModuleType module,
            @RequestParam Status status,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return repo.findByModuleTypeAndStatusAndSubmittedDateBetween(module, status, fromDate, toDate)
                .stream()
                .map(a -> new ApplicationDto(
                        a.getId(), a.getModuleType(), a.getStatus(),
                        a.getApplicantName(), a.getReferenceNo(),
                        a.getSubmittedDate(), a.getLastUpdated(), a.getRemarks()))
                .toList();
    }

    @GetMapping("/summary")
    public Map<String, Object> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        Map<ModuleType, Map<Status, Long>> grid = new LinkedHashMap<>();
        for (ModuleType m : ModuleType.values()) {
            Map<Status, Long> inner = new LinkedHashMap<>();
            for (Status s : Status.values()) inner.put(s, 0L);
            grid.put(m, inner);
        }

        for (Object[] row : repo.aggregateByModuleAndStatus(fromDate, toDate)) {
            ModuleType m = (ModuleType) row[0];
            Status s = (Status) row[1];
            Long cnt = (Long) row[2];
            grid.get(m).put(s, cnt);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        grid.forEach((m, map) -> {
            Map<String, Long> statusMap = map.entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().name(),
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new
                    ));
            out.put(m.name(), statusMap);
        });

        return out;
    }

    // ✅ Excel Export Endpoint
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToExcel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) throws IOException {

        List<ApplicationDto> applications = repo.findBySubmittedDateBetween(fromDate, toDate)
                .stream()
                .map(a -> new ApplicationDto(
                        a.getId(), a.getModuleType(), a.getStatus(),
                        a.getApplicantName(), a.getReferenceNo(),
                        a.getSubmittedDate(), a.getLastUpdated(), a.getRemarks()))
                .toList();

        // Create Excel workbook
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Applications");

        // Header row
        Row header = sheet.createRow(0);
        String[] columns = {"ID", "Module", "Status", "Applicant", "Reference No", "Submitted Date", "Last Updated", "Remarks"};
        for (int i = 0; i < columns.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columns[i]);
        }

        // Data rows
        int rowIdx = 1;
        for (ApplicationDto app : applications) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(app.id());
            row.createCell(1).setCellValue(app.moduleType().name());
            row.createCell(2).setCellValue(app.status().name());
            row.createCell(3).setCellValue(app.applicantName());
            row.createCell(4).setCellValue(app.referenceNo());
            row.createCell(5).setCellValue(app.submittedDate().toString());
            row.createCell(6).setCellValue(app.lastUpdated() != null ? app.lastUpdated().toString() : "");
            row.createCell(7).setCellValue(app.remarks() != null ? app.remarks() : "");
        }

        // ❌ Removed sheet.autoSizeColumn(i) (causing server crash on Railway)

        // Write to byte array
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        // Return response
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=applications.xlsx")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(out.toByteArray());
    }
}
