package org.example.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.user.dto.admin.AdminReportDto;
import org.example.user.dto.report.CreateReportRequest;
import org.example.user.service.admin.AdminReportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/social/reports")
@RequiredArgsConstructor
public class ReportController {

    private final AdminReportService adminReportService;

    @PostMapping
    public ResponseEntity<AdminReportDto> create(
            @RequestHeader("X-User-Id") Long reporterId,
            @Valid @RequestBody CreateReportRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminReportService.create(reporterId, request));
    }
}
