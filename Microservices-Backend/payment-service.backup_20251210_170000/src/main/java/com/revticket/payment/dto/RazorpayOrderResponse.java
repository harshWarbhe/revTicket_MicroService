package com.revticket.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RazorpayOrderResponse {
    private String orderId;
    private String currency;
    private Integer amount;
    private String key;
    
    public RazorpayOrderResponse(String orderId, String currency, Object amount, String key) {
        this.orderId = orderId;
        this.currency = currency;
        this.amount = amount instanceof Integer ? (Integer) amount : Integer.parseInt(amount.toString());
        this.key = key;
    }
}
