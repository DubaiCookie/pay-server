package com.payserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentConfirmDto {
    private String paymentKey;
    private Long orderId;           // DB에 저장된 실제 orderId (숫자)
    private String tossOrderId;     // Toss에 전달한 orderId (ORDER-xxx-xxx 형식)
    private Long amount;
}
