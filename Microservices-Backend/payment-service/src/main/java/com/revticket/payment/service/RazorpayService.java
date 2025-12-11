package com.revticket.payment.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.revticket.payment.dto.RazorpayOrderRequest;
import com.revticket.payment.dto.RazorpayOrderResponse;
import com.revticket.payment.dto.RazorpayVerificationRequest;
import com.revticket.payment.entity.Booking;
import com.revticket.payment.entity.Payment;
import com.revticket.payment.entity.Showtime;
import com.revticket.payment.entity.User;
import com.revticket.payment.repository.BookingRepository;
import com.revticket.payment.repository.PaymentRepository;
import com.revticket.payment.repository.ShowtimeRepository;
import com.revticket.payment.repository.UserRepository;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

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

        // Verify signature
        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", request.getRazorpayOrderId());
        options.put("razorpay_payment_id", request.getRazorpayPaymentId());
        options.put("razorpay_signature", request.getRazorpaySignature());

        boolean isValidSignature = Utils.verifyPaymentSignature(options, razorpayKeySecret);
        if (!isValidSignature) {
            throw new RuntimeException("Invalid payment signature");
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
            logger.error("Showtime not found: {}", request.getShowtimeId());
            throw new RuntimeException("Showtime not found: " + request.getShowtimeId());
        }

        // Book seats
        List<com.revticket.payment.entity.Seat> showtimeSeats = seatRepository.findByShowtimeId(request.getShowtimeId());
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
        if (showtime == null) return;

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
        try {
            logger.info("Fetching showtime from showtime-service: {}", showtimeId);
            Showtime showtime = restTemplate.getForObject(
                "http://showtime-service/api/showtimes/" + showtimeId,
                Showtime.class
            );
            logger.info("Successfully fetched showtime: {}", showtimeId);
            return showtime;
        } catch (Exception e) {
            logger.warn("Failed to fetch showtime from service: {}, falling back to local DB", e.getMessage());
            return showtimeRepository.findById(showtimeId).orElse(null);
        }
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
