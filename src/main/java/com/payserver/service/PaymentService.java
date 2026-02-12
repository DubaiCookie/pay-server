package com.payserver.service;

import com.payserver.client.TossPaymentClient;
import com.payserver.dto.*;
import com.payserver.entity.Payment;
import com.payserver.entity.PaymentStatus;
import com.payserver.kafka.KafkaProducer;
import com.payserver.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    /**
     * 결제 준비 (orderId 생성 및 DB 저장)
     */
    @Transactional
    public Payment createPayment(PaymentRequestDto request) {
        String orderId = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .orderId(orderId)
                .orderName(request.getOrderName())
                .amount(request.getAmount())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: orderId={}, userId={}, amount={}",
                orderId, request.getUserId(), request.getAmount());

        return savedPayment;
    }

    /**
     * 결제 승인 (Toss Payment API 호출)
     */
    @Transactional
    public Payment confirmPayment(PaymentConfirmDto confirmDto) {
        try {
            // Toss Payment API 호출
            TossPaymentConfirmRequest tossRequest = TossPaymentConfirmRequest.builder()
                    .paymentKey(confirmDto.getPaymentKey())
                    .orderId(confirmDto.getOrderId())
                    .amount(confirmDto.getAmount())
                    .build();

            TossPaymentResponse tossResponse = tossPaymentClient.confirmPayment(tossRequest);

            // DB 업데이트
            Payment payment = paymentRepository.findByOrderId(confirmDto.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + confirmDto.getOrderId()));

            payment.setPaymentKey(tossResponse.getPaymentKey());
            payment.setPaymentMethod(tossResponse.getMethod());
            payment.setPaymentStatus(PaymentStatus.COMPLETED);

            Payment updatedPayment = paymentRepository.save(payment);
            log.info("Payment confirmed: paymentKey={}, orderId={}",
                    tossResponse.getPaymentKey(), confirmDto.getOrderId());

            // Kafka 이벤트 전송
            kafkaProducer.sendPaymentCompletedEvent(
                    payment.getUserId(),
                    payment.getPaymentId(),
                    payment.getOrderId(),
                    payment.getAmount()
            );

            return updatedPayment;

        } catch (Exception e) {
            log.error("Payment confirmation failed: orderId={}", confirmDto.getOrderId(), e);

            // 결제 실패 처리
            Payment payment = paymentRepository.findByOrderId(confirmDto.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Payment not found"));
            payment.setPaymentStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);

            throw new RuntimeException("Payment confirmation failed", e);
        }
    }

    /**
     * 결제 조회 (orderId로)
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + orderId));
    }

    /**
     * 결제 조회 (paymentId로)
     */
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + paymentId));
    }

    /**
     * 사용자별 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }
}
