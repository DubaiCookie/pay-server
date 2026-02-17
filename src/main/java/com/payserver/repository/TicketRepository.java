package com.payserver.repository;

import com.payserver.entity.Ticket;
import com.payserver.entity.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Optional<Ticket> findByTicketType(TicketType ticketType);
}
