package com.payserver.dto;

import com.payserver.entity.TicketType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {
    private Long userId;
    private TicketType ticketType;
    private LocalDate availableDate;
    private Integer ticketQuantity;
}
