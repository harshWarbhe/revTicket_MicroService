package com.revticket.dashboard.controller;

import com.revticket.dashboard.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/admin/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportsController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getReportSummary(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        Map<String, Object> summary = new HashMap<>();

        try {
            // Get all bookings using DashboardService (which uses Feign Client with Auth)
            List<Object> bookings = dashboardService.getAllBookings();

            int totalBookings = bookings != null ? bookings.size() : 0;

            // Calculate revenue (simplified - in production, sum actual booking amounts)
            double totalRevenue = totalBookings * 250.0; // Average ticket price

            summary.put("totalBookings", totalBookings);
            summary.put("totalRevenue", totalRevenue);
            summary.put("averageBookingValue", totalBookings > 0 ? totalRevenue / totalBookings : 0);
            summary.put("period", Map.of(
                    "from", fromDate != null ? fromDate : LocalDate.now().minusMonths(1).toString(),
                    "to", toDate != null ? toDate : LocalDate.now().toString()));

        } catch (Exception e) {
            // Return empty summary if services are unavailable
            summary.put("totalBookings", 0);
            summary.put("totalRevenue", 0.0);
            summary.put("averageBookingValue", 0.0);
            summary.put("error", "Unable to fetch data: " + e.getMessage());
        }

        return ResponseEntity.ok(summary);
    }

    @GetMapping("/bookings")
    public ResponseEntity<Map<String, Object>> getBookingsReport(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Get all bookings using DashboardService
            List<Object> bookingList = dashboardService.getAllBookings();

            // Simple pagination
            int start = page * size;
            int end = Math.min(start + size, bookingList.size());
            List<Object> paginatedBookings = start < bookingList.size()
                    ? bookingList.subList(start, end)
                    : new ArrayList<>();

            response.put("content", paginatedBookings);
            response.put("totalElements", bookingList.size());
            response.put("totalPages", (int) Math.ceil((double) bookingList.size() / size));
            response.put("size", size);
            response.put("number", page);

        } catch (Exception e) {
            // Return empty result if service is unavailable
            response.put("content", new ArrayList<>());
            response.put("totalElements", 0);
            response.put("totalPages", 0);
            response.put("size", size);
            response.put("number", page);
            response.put("error", "Unable to fetch bookings: " + e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/revenue-trend")
    public ResponseEntity<List<Map<String, Object>>> getRevenueTrend(
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {

        List<Map<String, Object>> trendData = new ArrayList<>();

        try {
            // Get bookings using DashboardService
            List<Object> bookings = dashboardService.getAllBookings();

            // Generate trend data (simplified - in production, group by actual dates)
            LocalDate start = fromDate != null ? LocalDate.parse(fromDate) : LocalDate.now().minusDays(7);
            LocalDate end = toDate != null ? LocalDate.parse(toDate) : LocalDate.now();

            int totalBookings = bookings != null ? bookings.size() : 0;
            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(start, end);

            for (int i = 0; i <= daysBetween; i++) {
                LocalDate date = start.plusDays(i);
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", date.toString());
                // Distribute bookings evenly across days (simplified)
                dataPoint.put("revenue", (totalBookings / Math.max(daysBetween, 1)) * 250.0);
                trendData.add(dataPoint);
            }

        } catch (Exception e) {
            // Return empty trend if service unavailable
        }

        return ResponseEntity.ok(trendData);
    }
}
