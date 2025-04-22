package com.foodDelivery.userService.serviceInterfaces;

import java.util.List;

public interface ReportService {
    // Generate PDF report for a single user
    byte[] generateUserReport(Long userId);
    // Generate PDF report for users with a specific role
    byte[] generateRoleBasedReport(String roleName);
    // Get list of available report types
    List<String> getAvailableReportTypes();
}