package com.payserver.repository;

import com.payserver.entity.Refund;
import com.payserver.entity.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentId(Long paymentId);

    Optional<Refund> findByPaymentIdAndRefundStatus(Long paymentId, RefundStatus refundStatus);
}
