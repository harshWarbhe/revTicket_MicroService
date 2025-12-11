package com.revticket.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.revticket.payment.dto.RazorpayOrderRequest;
import com.revticket.payment.dto.RazorpayOrderResponse;
import com.revticket.payment.dto.RazorpayVerificationRequest;
import com.revticket.payment.dto.ShowtimeResponse;
import com.revticket.payment.entity.Booking;
import com.revticket.payment.entity.Movie;
import com.revticket.payment.entity.Payment;
import com.revticket.payment.entity.Showtime;
import com.revticket.payment.entity.Theater;
import com.revticket.payment.entity.User;
import com.revticket.payment.repository.BookingRepository;
import com.revticket.payment.repository.MovieRepository;
import com.revticket.payment.repository.PaymentRepository;
import com.revticket.payment.repository.ShowtimeRepository;
import com.revticket.payment.repository.TheaterRepository;
import com.revticket.payment.repository.UserRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class RazorpayService {
    private static final Logger logger = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TheaterRepository theaterRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private com.revticket.payment.repository.SeatRepository seatRepository;

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.revticket.payment.repository.ScreenRepository screenRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${app.gateway-url:http://localhost:8080}")
    private String gatewayUrl;

    public RazorpayOrderResponse createOrder(RazorpayOrderRequest request) throws RazorpayException {
        RazorpayClient razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", (int) (request.getAmount() * 100));
        orderRequest.put("currency", request.getCurrency());
        orderRequest.put("receipt", "order_" + System.currentTimeMillis());

        Order order = razorpayClient.orders.create(orderRequest);

        return new RazorpayOrderResponse(
                order.get("id"),
                order.get("currency"),
                order.get("amount"),
                razorpayKeyId);
    }

    @Transactional
    public Booking verifyPaymentAndCreateBooking(String userId, RazorpayVerificationRequest request) throws Exception {
        // Check if payment already processed
        var existingPayment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId());
        if (existingPayment.isPresent() && existingPayment.get().getStatus() == Payment.PaymentStatus.SUCCESS) {
            return existingPayment.get().getBooking();
        }

        // Verify signature (skip for test mode)
        // Test mode: if signature starts with "test_" or order ID starts with
        // "order_test", skip verification
        boolean isTestMode = request.getRazorpaySignature().startsWith("test_") ||
                request.getRazorpayOrderId().startsWith("order_test") ||
                request.getRazorpayOrderId().startsWith("order_Mock");

        if (!isTestMode) {
            JSONObject options = new JSONObject();
            options.put("razorpay_order_id", request.getRazorpayOrderId());
            options.put("razorpay_payment_id", request.getRazorpayPaymentId());
            options.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValidSignature = Utils.verifyPaymentSignature(options, razorpayKeySecret);
            if (!isValidSignature) {
                logger.warn("Invalid payment signature for order: {}", request.getRazorpayOrderId());
                throw new RuntimeException("Invalid payment signature");
            }
        } else {
            logger.info("Test mode: Skipping signature verification for order: {}", request.getRazorpayOrderId());
        }

        // Get or create user
        User user = userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setEmail(request.getCustomerEmail());
            newUser.setName(request.getCustomerName());
            newUser.setPhone(request.getCustomerPhone());
            newUser.setPassword("");
            newUser.setRole(User.Role.USER);
            return userRepository.save(newUser);
        });

        Showtime showtime = getShowtimeFromService(request.getShowtimeId());
        if (showtime == null) {
            logger.error("Showtime not found for ID: {} | Request seats: {} | Total amount: {}",
                    request.getShowtimeId(), request.getSeats(), request.getTotalAmount());
            throw new RuntimeException("Showtime verification failed: ID " + request.getShowtimeId()
                    + " not found in system. Please refresh and select seats again.");
        }

        // Book seats
        List<com.revticket.payment.entity.Seat> showtimeSeats = seatRepository
                .findByShowtimeId(request.getShowtimeId());
        for (String seatId : request.getSeats()) {
            com.revticket.payment.entity.Seat seat = showtimeSeats.stream()
                    .filter(s -> seatId.equals(s.getId()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Seat not found: " + seatId));

            if (seat.getIsBooked()) {
                throw new RuntimeException("Seat is already booked");
            }

            seat.setIsBooked(true);
            seat.setIsHeld(false);
            seat.setHoldExpiry(null);
            seat.setSessionId(null);
            seatRepository.save(seat);
        }

        showtime.setAvailableSeats(Math.max(0, showtime.getAvailableSeats() - request.getSeats().size()));
        showtimeRepository.save(showtime);

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setSeats(request.getSeats());
        booking.setSeatLabels(request.getSeatLabels());
        booking.setTotalAmount(request.getTotalAmount());
        booking.setCustomerName(request.getCustomerName());
        booking.setCustomerEmail(request.getCustomerEmail());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setTicketNumber("TKT-" + System.currentTimeMillis());
        booking.setPaymentMethod("RAZORPAY");
        booking.setScreenName(getScreenName(showtime.getScreen()));

        booking = bookingRepository.save(booking);

        // Create payment record
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getTotalAmount());
        payment.setPaymentMethod(Payment.PaymentMethod.UPI);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setRazorpayOrderId(request.getRazorpayOrderId());
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setTransactionId(request.getRazorpayPaymentId());

        paymentRepository.save(payment);

        // Send email notifications
        if (settingsService.areEmailNotificationsEnabled()) {
            try {
                emailService.sendBookingConfirmation(booking);
                emailService.sendAdminNewBookingNotification(booking);
            } catch (Exception e) {
                System.err.println("Failed to send email notifications: " + e.getMessage());
            }
        }

        return booking;
    }

    @Transactional
    public void handlePaymentFailure(String userId, RazorpayVerificationRequest request) {
        User user = userRepository.findById(userId).orElseGet(() -> {
            User newUser = new User();
            newUser.setId(userId);
            newUser.setEmail(request.getCustomerEmail());
            newUser.setName(request.getCustomerName());
            newUser.setPhone(request.getCustomerPhone());
            newUser.setPassword("");
            newUser.setRole(User.Role.USER);
            return userRepository.save(newUser);
        });

        Showtime showtime = getShowtimeFromService(request.getShowtimeId());
        if (showtime == null)
            return;

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setShowtime(showtime);
        booking.setSeats(request.getSeats());
        booking.setSeatLabels(request.getSeatLabels());
        booking.setTotalAmount(request.getTotalAmount());
        booking.setCustomerName(request.getCustomerName());
        booking.setCustomerEmail(request.getCustomerEmail());
        booking.setCustomerPhone(request.getCustomerPhone());
        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setPaymentMethod("RAZORPAY");

        booking = bookingRepository.save(booking);

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(request.getTotalAmount());
        payment.setPaymentMethod(Payment.PaymentMethod.UPI);
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setRazorpayOrderId(request.getRazorpayOrderId());

        paymentRepository.save(payment);
    }

    private Showtime getShowtimeFromService(String showtimeId) {
        if (showtimeId == null || showtimeId.trim().isEmpty()) {
            logger.error("Showtime ID is null or empty");
            return null;
        }

        // First, check local database
        Showtime localShowtime = showtimeRepository.findById(showtimeId).orElse(null);
        if (localShowtime != null) {
            logger.info("Showtime {} found in local database", showtimeId);
            return localShowtime;
        }

        // If not found locally, fetch from showtime service
        ShowtimeResponse showtimeResponse = null;

        try {
            logger.info("Fetching showtime from showtime-service: {}", showtimeId);
            showtimeResponse = restTemplate.getForObject(
                    "http://showtime-service/api/showtimes/" + showtimeId,
                    ShowtimeResponse.class);
            if (showtimeResponse != null) {
                logger.info("Successfully fetched showtime from service: {}", showtimeId);
            }
        } catch (RestClientException e) {
            logger.warn("Service discovery failed for ID {}: {}", showtimeId, e.getMessage());
        } catch (Exception e) {
            logger.warn("Unexpected error fetching from service discovery: {}", e.getMessage());
        }

        // Try gateway fallback if service discovery failed
        if (showtimeResponse == null) {
            try {
                logger.info("Attempting gateway fallback for showtime: {}", showtimeId);
                showtimeResponse = restTemplate.getForObject(
                        gatewayUrl + "/api/showtimes/" + showtimeId,
                        ShowtimeResponse.class);
                if (showtimeResponse != null) {
                    logger.info("Successfully fetched showtime via gateway: {}", showtimeId);
                }
            } catch (RestClientException e) {
                logger.warn("Gateway fallback failed for ID {}: {}", showtimeId, e.getMessage());
            } catch (Exception e) {
                logger.warn("Unexpected error fetching from gateway: {}", e.getMessage());
            }
        }

        if (showtimeResponse == null) {
            logger.error("Showtime {} not found in any source (service discovery or gateway)", showtimeId);
            return null;
        }

        // Map ShowtimeResponse to Showtime entity and save to local database
        try {
            Showtime showtime = mapShowtimeResponseToEntity(showtimeResponse);
            showtime = showtimeRepository.save(showtime);
            logger.info("Successfully saved showtime {} to local database", showtimeId);
            return showtime;
        } catch (Exception e) {
            logger.error("Failed to map and save showtime {}: {}", showtimeId, e.getMessage(), e);
            return null;
        }
    }

    private Showtime mapShowtimeResponseToEntity(ShowtimeResponse response) {
        Showtime showtime = new Showtime();
        showtime.setId(response.getId());
        showtime.setShowDateTime(response.getShowDateTime());
        showtime.setTicketPrice(response.getTicketPrice());
        showtime.setTotalSeats(response.getTotalSeats());
        showtime.setAvailableSeats(response.getAvailableSeats());
        showtime.setScreen(response.getScreen());

        // Map status
        if (response.getStatus() != null) {
            try {
                showtime.setStatus(Showtime.ShowStatus.valueOf(response.getStatus()));
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status value: {}, defaulting to ACTIVE", response.getStatus());
                showtime.setStatus(Showtime.ShowStatus.ACTIVE);
            }
        } else {
            showtime.setStatus(Showtime.ShowStatus.ACTIVE);
        }

        // Get or create Movie entity
        Movie movie = movieRepository.findById(response.getMovieId()).orElse(null);
        if (movie == null && response.getMovie() != null) {
            movie = new Movie();
            movie.setId(response.getMovieId());
            movie.setTitle(response.getMovie().getTitle() != null ? response.getMovie().getTitle() : "Unknown");
            movie.setLanguage(response.getMovie().getLanguage());
            movie.setDuration(response.getMovie().getDuration() != null ? response.getMovie().getDuration() : 120); // Default
                                                                                                                    // 120
                                                                                                                    // minutes
            movie.setPosterUrl(response.getMovie().getPosterUrl());
            movie.setReleaseDate(java.time.LocalDate.now()); // Set current date as default
            movie = movieRepository.save(movie);
            logger.info("Created movie entity: {}", movie.getId());
        } else if (movie == null) {
            // Create minimal movie if movie summary is not available
            movie = new Movie();
            movie.setId(response.getMovieId());
            movie.setTitle("Unknown Movie");
            movie.setDuration(120); // Required field
            movie.setReleaseDate(java.time.LocalDate.now()); // Required field
            movie = movieRepository.save(movie);
            logger.warn("Created minimal movie entity without details: {}", movie.getId());
        }
        showtime.setMovie(movie);

        // Get or create Theater entity
        Theater theater = theaterRepository.findById(response.getTheaterId()).orElse(null);
        if (theater == null && response.getTheater() != null) {
            theater = new Theater();
            theater.setId(response.getTheaterId());
            theater.setName(response.getTheater().getName() != null ? response.getTheater().getName() : "Unknown");
            theater.setLocation(response.getTheater().getLocation());
            theater.setAddress(response.getTheater().getAddress());
            theater.setTotalScreens(response.getTheater().getTotalScreens());
            theater = theaterRepository.save(theater);
            logger.info("Created theater entity: {}", theater.getId());
        } else if (theater == null) {
            // Create minimal theater if theater summary is not available
            theater = new Theater();
            theater.setId(response.getTheaterId());
            theater.setName("Unknown Theater");
            theater.setLocation("Unknown");
            theater.setAddress("Unknown");
            theater = theaterRepository.save(theater);
            logger.warn("Created minimal theater entity without details: {}", theater.getId());
        }
        showtime.setTheater(theater);

        return showtime;
    }

    private String getScreenName(String screenId) {
        if (screenId == null || screenId.isEmpty()) {
            return "Screen";
        }
        return screenRepository.findById(screenId)
                .map(screen -> screen.getName())
                .orElse("Screen");
    }
}
