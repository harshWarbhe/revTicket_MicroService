package com.revticket.payment.service;

import com.revticket.payment.entity.Booking;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Autowired
    private SettingsService settingsService;

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetToken;
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("RevTicket - Password Reset Request");
        message.setText(buildResetEmailBody(resetUrl));
        
        mailSender.send(message);
    }

    private String buildResetEmailBody(String resetUrl) {
        return "Hello,\n\n" +
               "You have requested to reset your password for your RevTicket account.\n\n" +
               "Please click the link below to reset your password:\n" +
               resetUrl + "\n\n" +
               "This link will expire in 1 hour.\n\n" +
               "If you did not request this password reset, please ignore this email.\n\n" +
               "Best regards,\n" +
               "RevTicket Team";
    }

    public void sendBookingConfirmation(Booking booking) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(booking.getCustomerEmail());
        message.setSubject("Booking Confirmed - " + booking.getShowtime().getMovie().getTitle());
        message.setText(buildBookingConfirmationBody(booking));
        mailSender.send(message);
    }

    public void sendCancellationConfirmation(Booking booking) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(booking.getCustomerEmail());
        message.setSubject("Booking Cancelled - " + booking.getShowtime().getMovie().getTitle());
        message.setText(buildCancellationBody(booking));
        mailSender.send(message);
    }

    private String buildBookingConfirmationBody(Booking booking) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String showtime = booking.getShowtime().getShowDateTime().format(formatter);
        String seats = booking.getSeatLabels() != null && !booking.getSeatLabels().isEmpty() 
            ? String.join(", ", booking.getSeatLabels()) 
            : String.join(", ", booking.getSeats());

        return "Dear " + booking.getCustomerName() + ",\n\n" +
               "Your booking has been confirmed!\n\n" +
               "Booking Details:\n" +
               "Ticket Number: " + booking.getTicketNumber() + "\n" +
               "Movie: " + booking.getShowtime().getMovie().getTitle() + "\n" +
               "Theater: " + booking.getShowtime().getTheater().getName() + "\n" +
               "Screen: " + booking.getScreenName() + "\n" +
               "Showtime: " + showtime + "\n" +
               "Seats: " + seats + "\n" +
               "Total Amount: ₹" + booking.getTotalAmount() + "\n\n" +
               "Please arrive 30 minutes before showtime.\n\n" +
               "View your ticket: " + frontendUrl + "/user/my-bookings\n\n" +
               "Enjoy your movie!\n\n" +
               "Best regards,\n" +
               "RevTicket Team";
    }

    private String buildCancellationBody(Booking booking) {
        return "Dear " + booking.getCustomerName() + ",\n\n" +
               "Your booking has been cancelled.\n\n" +
               "Booking Details:\n" +
               "Ticket Number: " + booking.getTicketNumber() + "\n" +
               "Movie: " + booking.getShowtime().getMovie().getTitle() + "\n" +
               "Refund Amount: ₹" + (booking.getRefundAmount() != null ? booking.getRefundAmount() : 0) + "\n\n" +
               "The refund will be processed within 5-7 business days.\n\n" +
               "Best regards,\n" +
               "RevTicket Team";
    }

    public void sendAdminNewUserNotification(String userName, String userEmail) {
        String adminEmail = settingsService.getSetting("siteEmail");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("New User Registration - RevTicket");
        message.setText("New user registered:\n\n" +
                       "Name: " + userName + "\n" +
                       "Email: " + userEmail + "\n\n" +
                       "Login to admin panel to view details.");
        mailSender.send(message);
    }

    public void sendAdminNewBookingNotification(Booking booking) {
        String adminEmail = settingsService.getSetting("siteEmail");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
        String showtime = booking.getShowtime().getShowDateTime().format(formatter);
        String seats = booking.getSeatLabels() != null && !booking.getSeatLabels().isEmpty() 
            ? String.join(", ", booking.getSeatLabels()) 
            : String.join(", ", booking.getSeats());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("New Booking - " + booking.getShowtime().getMovie().getTitle());
        message.setText("New booking received:\n\n" +
                       "Ticket Number: " + booking.getTicketNumber() + "\n" +
                       "Customer: " + booking.getCustomerName() + " (" + booking.getCustomerEmail() + ")\n" +
                       "Movie: " + booking.getShowtime().getMovie().getTitle() + "\n" +
                       "Theater: " + booking.getShowtime().getTheater().getName() + "\n" +
                       "Screen: " + booking.getScreenName() + "\n" +
                       "Showtime: " + showtime + "\n" +
                       "Seats: " + seats + "\n" +
                       "Amount: ₹" + booking.getTotalAmount() + "\n\n" +
                       "Login to admin panel to view details.");
        mailSender.send(message);
    }

    public void sendAdminCancellationRequestNotification(Booking booking, String reason) {
        String adminEmail = settingsService.getSetting("siteEmail");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(adminEmail);
        message.setSubject("Cancellation Request - " + booking.getTicketNumber());
        message.setText("Cancellation request received:\n\n" +
                       "Ticket Number: " + booking.getTicketNumber() + "\n" +
                       "Customer: " + booking.getCustomerName() + " (" + booking.getCustomerEmail() + ")\n" +
                       "Movie: " + booking.getShowtime().getMovie().getTitle() + "\n" +
                       "Reason: " + reason + "\n\n" +
                       "Login to admin panel to approve/reject.");
        mailSender.send(message);
    }
}
