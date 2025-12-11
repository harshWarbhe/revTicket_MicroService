package com.revticket.dashboard.service;

import com.revticket.dashboard.client.*;
import com.revticket.dashboard.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private BookingServiceClient bookingServiceClient;

    @Autowired
    private PaymentServiceClient paymentServiceClient;

    @Autowired
    private MovieServiceClient movieServiceClient;

    @Autowired
    private TheaterServiceClient theaterServiceClient;

    @Autowired
    private ShowtimeServiceClient showtimeServiceClient;

    @Autowired
    private ReviewServiceClient reviewServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    public SystemOverviewDTO getSystemOverview() {
        SystemOverviewDTO overview = new SystemOverviewDTO();

        try {
            Map<String, Object> userStats = userServiceClient.getUserStats();
            overview.setTotalUsers(getLong(userStats, "totalUsers"));
        } catch (Exception e) {
            overview.setTotalUsers(0L);
        }

        try {
            Map<String, Object> bookingStats = bookingServiceClient.getBookingStats();
            overview.setTotalBookings(getLong(bookingStats, "totalBookings"));
            overview.setActiveBookings(getLong(bookingStats, "confirmedBookings"));
            overview.setCancelledBookings(getLong(bookingStats, "cancelledBookings"));
        } catch (Exception e) {
            overview.setTotalBookings(0L);
            overview.setActiveBookings(0L);
            overview.setCancelledBookings(0L);
        }

        try {
            Map<String, Object> movieStats = movieServiceClient.getMovieStats();
            overview.setTotalMovies(getLong(movieStats, "totalMovies"));
        } catch (Exception e) {
            overview.setTotalMovies(0L);
        }

        try {
            Map<String, Object> theaterStats = theaterServiceClient.getTheaterStats();
            overview.setTotalTheaters(getLong(theaterStats, "totalTheaters"));
        } catch (Exception e) {
            overview.setTotalTheaters(0L);
        }

        try {
            Map<String, Object> showtimeStats = showtimeServiceClient.getShowtimeStats();
            overview.setTotalShowtimes(getLong(showtimeStats, "totalShowtimes"));
        } catch (Exception e) {
            overview.setTotalShowtimes(0L);
        }

        try {
            Map<String, Object> reviewStats = reviewServiceClient.getReviewStats();
            overview.setTotalReviews(getLong(reviewStats, "totalReviews"));
        } catch (Exception e) {
            overview.setTotalReviews(0L);
        }

        try {
            Map<String, Object> paymentStats = paymentServiceClient.getPaymentStats();
            overview.setTotalRevenue(getDouble(paymentStats, "totalRevenue"));
        } catch (Exception e) {
            overview.setTotalRevenue(0.0);
        }

        return overview;
    }

    public RevenueStatsDTO getRevenueStats() {
        try {
            Map<String, Object> stats = paymentServiceClient.getPaymentStats();
            return new RevenueStatsDTO(
                    getDouble(stats, "totalRevenue"),
                    getDouble(stats, "convenienceFees"),
                    getDouble(stats, "gstAmount"),
                    getDouble(stats, "netRevenue"),
                    getLong(stats, "totalTransactions"));
        } catch (Exception e) {
            return new RevenueStatsDTO(0.0, 0.0, 0.0, 0.0, 0L);
        }
    }

    public BookingStatsDTO getBookingStats() {
        try {
            Map<String, Object> stats = bookingServiceClient.getBookingStats();
            return new BookingStatsDTO(
                    getLong(stats, "totalBookings"),
                    getLong(stats, "confirmedBookings"),
                    getLong(stats, "cancelledBookings"),
                    getLong(stats, "pendingBookings"),
                    getDouble(stats, "averageBookingValue"),
                    getLong(stats, "totalSeatsBooked"));
        } catch (Exception e) {
            return new BookingStatsDTO(0L, 0L, 0L, 0L, 0.0, 0L);
        }
    }

    public Map<String, Object> getMovieStats() {
        try {
            return movieServiceClient.getMovieStats();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public TheaterStatsDTO getTheaterStats() {
        try {
            Map<String, Object> stats = theaterServiceClient.getTheaterStats();
            return new TheaterStatsDTO(
                    getLong(stats, "totalTheaters"),
                    getLong(stats, "activeTheaters"),
                    getLong(stats, "totalScreens"),
                    getLong(stats, "totalSeats"));
        } catch (Exception e) {
            return new TheaterStatsDTO(0L, 0L, 0L, 0L);
        }
    }

    public ShowtimeStatsDTO getShowtimeStats() {
        try {
            Map<String, Object> stats = showtimeServiceClient.getShowtimeStats();
            return new ShowtimeStatsDTO(
                    getLong(stats, "totalShowtimes"),
                    getLong(stats, "upcomingShowtimes"),
                    getLong(stats, "completedShowtimes"),
                    getDouble(stats, "averageOccupancy"));
        } catch (Exception e) {
            return new ShowtimeStatsDTO(0L, 0L, 0L, 0.0);
        }
    }

    public Map<String, Object> getReviewStats() {
        try {
            return reviewServiceClient.getReviewStats();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public java.util.List<Object> getAllBookings() {
        try {
            return bookingServiceClient.getAllBookings();
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    public AnalyticsResponse getFullReport() {
        return new AnalyticsResponse(
                getSystemOverview(),
                getRevenueStats(),
                getBookingStats(),
                getTheaterStats(),
                getShowtimeStats());
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
