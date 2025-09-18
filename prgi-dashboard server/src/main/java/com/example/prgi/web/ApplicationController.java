package com.example.prgi.web;

import com.example.prgi.domain.ModuleType;
import com.example.prgi.domain.Status;
import com.example.prgi.repo.ApplicationRepository;
import com.example.prgi.web.dto.ApplicationDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/applications")

public class ApplicationController {
    private final ApplicationRepository repo;

    public ApplicationController(ApplicationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<ApplicationDto> list(
            @RequestParam(required = false) ModuleType module,
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        // If all filters are provided → filtered query
        if (module != null && status != null && fromDate != null && toDate != null) {
            return repo.findByModuleTypeAndStatusAndSubmittedDateBetween(module, status, fromDate, toDate)
                    .stream()
                    .map(a -> new ApplicationDto(
                            a.getId(), a.getModuleType(), a.getStatus(),
                            a.getApplicantName(), a.getReferenceNo(),
                            a.getSubmittedDate(), a.getLastUpdated(), a.getRemarks()))
                    .toList();
        }

        // Otherwise → return all applications
        return repo.findAll().stream()
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
}
