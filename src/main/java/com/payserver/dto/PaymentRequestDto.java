package com.payserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private Long userId;
    private String orderName;
    private Long amount;
    private Integer ticketQuantity;  // 티켓 구매 수량 (선택적, 기본값 1)
}
