package com.foodDelivery.userService.serviceImplementation;

import com.foodDelivery.userService.modal.Role;
import com.foodDelivery.userService.modal.User;
import com.foodDelivery.userService.repository.UserRepository;
import com.foodDelivery.userService.serviceInterfaces.ReportService;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final UserRepository userRepository;

    @Override
    public byte[] generateUserReport(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        return generatePdfForUser(user);
    }

    @Override
    public byte[] generateRoleBasedReport(String roleName) {
        String formattedRole = roleName.toUpperCase();
        List<User> users = userRepository.findByRolesName(formattedRole);
        return generatePdfForUserList(users, roleName);
    }

    @Override
    public List<String> getAvailableReportTypes() {
        return Arrays.asList("USER", "ADMIN", "DRIVER", "RESTAURANT_OWNER", "CUSTOMER");
    }

    private byte[] generatePdfForUser(User user) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();
            addMetaData(document, "User Report: " + user.getUsername());

            // Add title
            Paragraph title = new Paragraph("User Profile Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Username: " + user.getUsername(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            document.add(Chunk.NEWLINE);

            // Create user details table
            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);

            // Add user information
            addTableRow(table, "ID", user.getId().toString());
            addTableRow(table, "Email", user.getEmail());
            addTableRow(table, "Full Name", user.getFirstName() + " " + user.getLastName());
            addTableRow(table, "Phone Number", user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
            addTableRow(table, "Address", user.getAddress() != null ? user.getAddress() : "N/A");

            // Roles
            String roles = user.getRoles().stream()
                    .map(Role::getName)
                    .map(r -> r.replace("ROLE_", ""))
                    .collect(Collectors.joining(", "));
            addTableRow(table, "Roles", roles);

            // Account status
            addTableRow(table, "Account Enabled", String.valueOf(user.isEnabled()));
            addTableRow(table, "Account Verified", String.valueOf(user.isVerified()));

            // Additional role-specific information
            if (roles.contains("DRIVER")) {
                addTableRow(table, "Vehicle Number", user.getVehicleNumber() != null ? user.getVehicleNumber() : "N/A");
            }

            // Location information if available
            if (user.getLatitude() != null && user.getLongitude() != null) {
                addTableRow(table, "Location", "Lat: " + user.getLatitude() + ", Long: " + user.getLongitude());
            }

            // Add timestamps
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            addTableRow(table, "Created At", user.getCreatedAt().format(formatter));
            if (user.getUpdatedAt() != null) {
                addTableRow(table, "Last Updated", user.getUpdatedAt().format(formatter));
            }

            document.add(table);

            // Add footer
            document.add(Chunk.NEWLINE);
            Paragraph footer = new Paragraph("Report generated on: " +
                    LocalDateTime.now().format(formatter),
                    FontFactory.getFont(FontFactory.HELVETICA, 10));
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF for user {}: {}", user.getUsername(), e.getMessage());
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    private byte[] generatePdfForUserList(List<User> users, String roleName) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate()); // Landscape for tables
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            document.open();

            // Add title
            Paragraph title = new Paragraph(roleName + " Users Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Add summary
            document.add(new Paragraph("Total users: " + users.size(),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(Chunk.NEWLINE);

            if (users.isEmpty()) {
                document.add(new Paragraph("No users found with role: " + roleName));
            } else {
                // Create users table
                PdfPTable table = new PdfPTable(7); // Adjust columns as needed
                table.setWidthPercentage(100);

                // Add headers
                String[] headers = {"ID", "Username", "Email", "Full Name", "Phone", "Status", "Created Date"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
                    cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    cell.setPadding(5);
                    table.addCell(cell);
                }

                // Add user rows
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                for (User user : users) {
                    table.addCell(user.getId().toString());
                    table.addCell(user.getUsername());
                    table.addCell(user.getEmail());
                    table.addCell(user.getFirstName() + " " + user.getLastName());
                    table.addCell(user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");

                    // Status
                    String status = user.isDisabled() ? "Disabled" : (user.isVerified() ? "Verified" : "Unverified");
                    table.addCell(status);

                    // Created date
                    table.addCell(user.getCreatedAt().format(formatter));
                }

                document.add(table);
            }

            // Add footer
            document.add(Chunk.NEWLINE);
            document.add(new Paragraph("Report generated on: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error generating PDF for {} users: {}", roleName, e.getMessage());
            throw new RuntimeException("Failed to generate role-based PDF report", e);
        }
    }

    private void addTableRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        keyCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        keyCell.setPadding(5);

        PdfPCell valueCell = new PdfPCell(new Phrase(value));
        valueCell.setPadding(5);

        table.addCell(keyCell);
        table.addCell(valueCell);
    }

    private void addMetaData(Document document, String title) {
        document.addTitle(title);
        document.addSubject("User Information Report");
        document.addKeywords("Food Delivery, User, Report");
        document.addAuthor("Food Delivery System");
        document.addCreator("Food Delivery System");
    }
}