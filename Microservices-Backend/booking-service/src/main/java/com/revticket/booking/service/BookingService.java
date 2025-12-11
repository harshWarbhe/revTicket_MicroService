package com.revticket.booking.service;

import com.revticket.booking.dto.BookingRequest;
import com.revticket.booking.dto.BookingResponse;
import com.revticket.booking.entity.Booking;
import com.revticket.booking.entity.Movie;
import com.revticket.booking.entity.Seat;
import com.revticket.booking.entity.Showtime;
import com.revticket.booking.entity.Theater;
import com.revticket.booking.entity.User;
import com.revticket.booking.repository.BookingRepository;
import com.revticket.booking.repository.SeatRepository;
import com.revticket.booking.repository.ShowtimeRepository;
import com.revticket.booking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private com.revticket.booking.repository.ScreenRepository screenRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public BookingResponse createBooking(String userId, BookingRequest request) {
        if (request.getSeats() == null || request.getSeats().isEmpty()) {
            throw new RuntimeException("No seats selected");
        }
        
        int maxSeats = settingsService.getMaxSeatsPerBooking();
        if (request.getSeats().size() > maxSeats) {
            throw new RuntimeException("Maximum " + maxSeats + " seats can be booked at once");
        }

        User user = userRepository.findById(Objects.requireNonNullElse(userId, ""))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Showtime showtime = showtimeRepository.findById(Objects.requireNonNullElse(request.getShowtimeId(), ""))
                .orElseThrow(() -> new RuntimeException("Showtime not found"));
        
        if (showtime.getShowDateTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Cannot book tickets for past showtimes");
        }

        List<Seat> showtimeSeats = seatRepository.findByShowtimeId(showtime.getId());
        
        // Validate seats by ID
        for (String seatId : request.getSeats()) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
            
            if (seat.getIsBooked() || seat.getIsHeld()) {
                throw new RuntimeException("Seat is no longer available: " + seat.getRow() + seat.getNumber());
            }
        }

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setSeats(request.getSeats());
        if (request.getSeatLabels() != null && !request.getSeatLabels().isEmpty()) {
            booking.setSeatLabels(request.getSeatLabels());
        }
        booking.setTotalAmount(request.getTotalAmount());
        booking.setTicketPriceSnapshot(showtime.getTicketPrice());
        booking.setScreenName(getScreenName(showtime.getScreen()));
        booking.setPaymentMethod("ONLINE");
        booking.setCustomerName(Objects.requireNonNullElse(request.getCustomerName(), ""));
        booking.setCustomerEmail(Objects.requireNonNullElse(request.getCustomerEmail(), ""));
        booking.setCustomerPhone(Objects.requireNonNullElse(request.getCustomerPhone(), ""));
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setTicketNumber("TKT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        booking.setQrCode("QR_" + UUID.randomUUID().toString());

        booking = bookingRepository.save(booking);

        // Mark seats as booked
        for (String seatId : request.getSeats()) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElse(null);
            if (seat != null) {
                seat.setIsBooked(true);
                seat.setIsHeld(false);
                seat.setHoldExpiry(null);
                seat.setSessionId(null);
                seatRepository.save(seat);
            }
        }

        showtime.setAvailableSeats(Math.max(0, showtime.getAvailableSeats() - request.getSeats().size()));
        showtimeRepository.save(showtime);

        // Send email notification
        boolean emailEnabled = settingsService.areEmailNotificationsEnabled();
        System.out.println("Email notifications enabled: " + emailEnabled);
        if (emailEnabled) {
            try {
                System.out.println("Sending booking confirmation to: " + booking.getCustomerEmail());
                emailService.sendBookingConfirmation(booking);
                System.out.println("✓ Booking confirmation email sent successfully");
            } catch (Exception e) {
                System.err.println("✗ Failed to send booking confirmation email: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Email notifications are disabled in settings");
        }

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String userId) {
        return bookingRepository.findByUserId(Objects.requireNonNullElse(userId, ""))
                .stream()
                .sorted((b1, b2) -> b2.getBookingDate().compareTo(b1.getBookingDate()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<BookingResponse> getBookingById(String id) {
        return bookingRepository.findById(Objects.requireNonNullElse(id, ""))
                .map(this::mapToResponse);
    }

    @Transactional
    public BookingResponse requestCancellation(String id, String reason) {
        Booking booking = bookingRepository.findById(Objects.requireNonNullElse(id, ""))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can request cancellation");
        }

        int cancellationHours = settingsService.getCancellationWindowHours();
        long hoursUntilShow = java.time.Duration.between(LocalDateTime.now(), booking.getShowtime().getShowDateTime()).toHours();
        if (hoursUntilShow < cancellationHours) {
            throw new RuntimeException("Cancellation not allowed. Must cancel at least " + cancellationHours + " hours before showtime");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLATION_PENDING);
        booking.setCancellationReason(Objects.requireNonNullElse(reason, ""));
        booking.setCancellationRequestedAt(LocalDateTime.now());
        
        booking = bookingRepository.save(booking);

        if (settingsService.areEmailNotificationsEnabled()) {
            try {
                emailService.sendAdminCancellationRequestNotification(booking, reason);
            } catch (Exception e) {
                System.err.println("Failed to send admin cancellation notification: " + e.getMessage());
            }
        }

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getCancellationRequests() {
        return bookingRepository.findAll()
                .stream()
                .filter(b -> b.getStatus() == Booking.BookingStatus.CANCELLATION_PENDING)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse cancelBooking(String id, String reason) {
        Booking booking = bookingRepository.findById(Objects.requireNonNullElse(id, ""))
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Booking is already cancelled");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        if (reason != null && !reason.isEmpty()) {
            booking.setCancellationReason(Objects.requireNonNullElse(reason, ""));
        }

        List<Seat> showtimeSeats = seatRepository.findByShowtimeId(booking.getShowtime().getId());
        for (String seatId : booking.getSeats()) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElse(null);
            if (seat != null) {
                seat.setIsBooked(false);
                seat.setIsHeld(false);
                seatRepository.save(seat);
            }
        }

        Showtime showtime = booking.getShowtime();
        showtime.setAvailableSeats(showtime.getAvailableSeats() + booking.getSeats().size());
        showtimeRepository.save(showtime);

        booking.setRefundAmount(calculateRefund(booking));
        booking.setRefundDate(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        if (settingsService.areEmailNotificationsEnabled()) {
            try {
                emailService.sendCancellationConfirmation(savedBooking);
            } catch (Exception e) {
                System.err.println("Failed to send cancellation email: " + e.getMessage());
            }
        }

        return mapToResponse(savedBooking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private Double calculateRefund(Booking booking) {
        return booking.getTotalAmount() * 0.9;
    }

    @Transactional
    public void deleteBooking(String id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNullElse(id, ""))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        List<Seat> showtimeSeats = seatRepository.findByShowtimeId(booking.getShowtime().getId());
        for (String seatId : booking.getSeats()) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElse(null);
            if (seat != null) {
                seat.setIsBooked(false);
                seat.setIsHeld(false);
                seatRepository.save(seat);
            }
        }

        Showtime showtime = booking.getShowtime();
        showtime.setAvailableSeats(showtime.getAvailableSeats() + booking.getSeats().size());
        showtimeRepository.save(showtime);

        bookingRepository.delete(booking);
    }

    @Transactional
    public BookingResponse scanBooking(String id) {
        Booking booking = bookingRepository.findById(Objects.requireNonNullElse(id, ""))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Cannot scan cancelled booking");
        }

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        return mapToResponse(bookingRepository.save(booking));
    }

    @Transactional
    public BookingResponse resignBooking(String id, List<String> newSeats) {
        Booking booking = bookingRepository.findById(Objects.requireNonNullElse(id, ""))
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new RuntimeException("Cannot reassign seats for cancelled booking");
        }

        List<Seat> showtimeSeats = seatRepository.findByShowtimeId(booking.getShowtime().getId());
        
        for (String seatId : booking.getSeats()) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElse(null);
            if (seat != null) {
                seat.setIsBooked(false);
                seatRepository.save(seat);
            }
        }

        for (String seatId : newSeats) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));
            if (seat.getIsBooked()) {
                throw new RuntimeException("Seat " + seat.getRow() + seat.getNumber() + " is already booked");
            }
        }

        Showtime showtime = booking.getShowtime();
        int seatDifference = newSeats.size() - booking.getSeats().size();
        showtime.setAvailableSeats(showtime.getAvailableSeats() - seatDifference);
        showtimeRepository.save(showtime);

        booking.setSeats(newSeats);
        for (String seatId : newSeats) {
            Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElse(null);
            if (seat != null) {
                seat.setIsBooked(true);
                seatRepository.save(seat);
            }
        }

        return mapToResponse(bookingRepository.save(booking));
    }

    private String getScreenName(String screenId) {
        if (screenId == null || screenId.isEmpty()) {
            return "Screen";
        }
        return screenRepository.findById(screenId)
                .map(screen -> screen.getName())
                .orElse(screenId);
    }

    private BookingResponse mapToResponse(Booking booking) {
        Showtime showtime = booking.getShowtime();
        Movie movie = showtime.getMovie();
        Theater theater = showtime.getTheater();

        return BookingResponse.builder()
                .id(Objects.requireNonNullElse(booking.getId(), ""))
                .userId(Objects.requireNonNullElse(booking.getUser().getId(), ""))
                .movieId(movie != null ? Objects.requireNonNullElse(movie.getId(), "") : "")
                .movieTitle(movie != null ? Objects.requireNonNullElse(movie.getTitle(), "") : "")
                .moviePosterUrl(movie != null ? Objects.requireNonNullElse(movie.getPosterUrl(), "") : "")
                .theaterId(theater != null ? Objects.requireNonNullElse(theater.getId(), "") : "")
                .theaterName(theater != null ? Objects.requireNonNullElse(theater.getName(), "") : "")
                .theaterLocation(theater != null ? Objects.requireNonNullElse(theater.getLocation(), "") : "")
                .showtimeId(Objects.requireNonNullElse(showtime.getId(), ""))
                .showtime(showtime.getShowDateTime())
                .screen(getScreenName(showtime.getScreen()))
                .ticketPrice(showtime.getTicketPrice())
                .seats(booking.getSeats())
                .seatLabels(booking.getSeatLabels())
                .totalAmount(booking.getTotalAmount())
                .bookingDate(booking.getBookingDate())
                .status(booking.getStatus())
                .customerName(Objects.requireNonNullElse(booking.getCustomerName(), ""))
                .customerEmail(Objects.requireNonNullElse(booking.getCustomerEmail(), ""))
                .customerPhone(Objects.requireNonNullElse(booking.getCustomerPhone(), ""))
                .paymentId(Objects.requireNonNullElse(booking.getPaymentId(), ""))
                .qrCode(Objects.requireNonNullElse(booking.getQrCode(), ""))
                .ticketNumber(Objects.requireNonNullElse(booking.getTicketNumber(), ""))
                .refundAmount(booking.getRefundAmount())
                .refundDate(booking.getRefundDate())
                .cancellationReason(Objects.requireNonNullElse(booking.getCancellationReason(), ""))
                .build();
    }
}
