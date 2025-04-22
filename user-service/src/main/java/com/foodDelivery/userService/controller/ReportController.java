package com.foodDelivery.userService.controller;

import com.foodDelivery.userService.serviceInterfaces.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generateUserReport(@PathVariable Long userId) {
        try {
            byte[] pdfContent = reportService.generateUserReport(userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", "user-report-" + userId + ".pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            log.error("Failed to generate user report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/roles/{roleName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> generateRoleBasedReport(@PathVariable String roleName) {
        try {
            byte[] pdfContent = reportService.generateRoleBasedReport(roleName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("filename", roleName + "-users-report.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);
        } catch (Exception e) {
            log.error("Failed to generate role-based report: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getAvailableReportTypes() {
        return ResponseEntity.ok(reportService.getAvailableReportTypes());
    }
}