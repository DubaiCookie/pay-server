package com.payserver.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEventDto {
    private Long userId;
    private Long paymentId;
    private String orderId;
    private Long amount;
    private String eventType;
}
