package com.revticket.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsResponse {
    private Double totalRevenue;
    private Long totalSuccessfulPayments;
    private Long failedPayments;
    private Double revenueLast7Days;
    private Double revenueLast30Days;
}
