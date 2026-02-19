package com.payserver.service;

import com.payserver.client.TossPaymentClient;
import com.payserver.dto.RefundRequestDto;
import com.payserver.dto.TossRefundRequest;
import com.payserver.dto.TossPaymentResponse;
import com.payserver.entity.Payment;
import com.payserver.entity.PaymentStatus;
import com.payserver.entity.Refund;
import com.payserver.entity.RefundStatus;
import com.payserver.kafka.KafkaProducer;
import com.payserver.repository.PaymentRepository;
import com.payserver.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    /**
     * 환불 처리
     */
    @Transactional
    public Refund processRefund(RefundRequestDto request) {
        // 멱등성 체크: 비관적 락으로 결제 조회
        Payment payment = paymentRepository.findByIdWithLock(request.getPaymentId())
                .orElseThrow(() -> new RuntimeException("Payment not found: " + request.getPaymentId()));

        // 이미 취소된 결제 → 기존 완료된 환불 반환
        if (payment.getPaymentStatus() == PaymentStatus.CANCELLED) {
            return refundRepository.findByPaymentIdAndRefundStatus(request.getPaymentId(), RefundStatus.COMPLETED)
                    .orElseThrow(() -> new RuntimeException("Refund record not found for cancelled payment: " + request.getPaymentId()));
        }

        try {
            if (payment.getPaymentStatus() != PaymentStatus.COMPLETED) {
                throw new RuntimeException("Payment is not completed: " + payment.getPaymentId());
            }

            if (payment.getPaymentKey() == null) {
                throw new RuntimeException("Payment key is null: " + payment.getPaymentId());
            }

            // 환불 기록 생성
            Refund refund = Refund.builder()
                    .paymentId(payment.getPaymentId())
                    .refundAmount(request.getRefundAmount())
                    .refundReason(request.getRefundReason())
                    .refundStatus(RefundStatus.PENDING)
                    .build();

            Refund savedRefund = refundRepository.save(refund);

            // Toss Payment API 환불 요청
            TossRefundRequest tossRequest = TossRefundRequest.builder()
                    .cancelReason(request.getRefundReason())
                    .cancelAmount(request.getRefundAmount())
                    .build();

            TossPaymentResponse tossResponse = tossPaymentClient.cancelPayment(
                    payment.getPaymentKey(),
                    tossRequest
            );

            // 환불 완료 처리
            savedRefund.setRefundStatus(RefundStatus.COMPLETED);
            refundRepository.save(savedRefund);

            // 결제 상태 업데이트
            payment.setPaymentStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            log.info("Refund completed: refundId={}, paymentId={}, amount={}",
                    savedRefund.getRefundId(), payment.getPaymentId(), request.getRefundAmount());

            // Kafka 이벤트 전송
            kafkaProducer.sendRefundCompletedEvent(
                    payment.getUserId(),
                    payment.getPaymentId(),
                    savedRefund.getRefundId(),
                    savedRefund.getRefundAmount()
            );

            return savedRefund;

        } catch (Exception e) {
            log.error("Refund failed: paymentId={}", request.getPaymentId(), e);

            // 환불 실패 처리
            Refund refund = Refund.builder()
                    .paymentId(request.getPaymentId())
                    .refundAmount(request.getRefundAmount())
                    .refundReason(request.getRefundReason())
                    .refundStatus(RefundStatus.FAILED)
                    .build();
            refundRepository.save(refund);

            throw new RuntimeException("Refund failed", e);
        }
    }

    /**
     * 환불 내역 조회 (결제 ID로)
     */
    @Transactional(readOnly = true)
    public List<Refund> getRefundsByPaymentId(Long paymentId) {
        return refundRepository.findByPaymentId(paymentId);
    }

    /**
     * 환불 조회 (환불 ID로)
     */
    @Transactional(readOnly = true)
    public Refund getRefundById(Long refundId) {
        return refundRepository.findById(refundId)
                .orElseThrow(() -> new RuntimeException("Refund not found: " + refundId));
    }
}
