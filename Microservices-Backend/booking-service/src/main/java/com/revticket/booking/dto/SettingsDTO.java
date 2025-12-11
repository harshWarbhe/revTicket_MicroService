package com.revticket.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettingsDTO {
    private String siteName;
    private String siteEmail;
    private String sitePhone;
    private String currency;
    private String timezone;
    private Integer bookingCancellationHours;
    private Double convenienceFeePercent;
    private Double gstPercent;
    private Integer maxSeatsPerBooking;
    private Boolean enableNotifications;
    private Boolean enableEmailNotifications;
    private Boolean enableSMSNotifications;
    private Boolean maintenanceMode;
}
