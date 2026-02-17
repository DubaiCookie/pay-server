package com.payserver.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ticket_management")
@Getter
@NoArgsConstructor
public class TicketManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_management_id")
    private Long ticketManagementId;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "stock", nullable = false)
    private Integer stock;
}
