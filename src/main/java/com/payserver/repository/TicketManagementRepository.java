package com.payserver.repository;

import com.payserver.entity.TicketManagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TicketManagementRepository extends JpaRepository<TicketManagement, Long> {
    Optional<TicketManagement> findByTicketIdAndAvailableAtBetween(Long ticketId, LocalDateTime start, LocalDateTime end);
}
