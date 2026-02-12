package com.payserver.service;

import com.payserver.client.TossPaymentClient;
import com.payserver.dto.*;
import com.payserver.entity.Order;
import com.payserver.entity.OrderStatus;
import com.payserver.entity.Payment;
import com.payserver.entity.PaymentStatus;
import com.payserver.kafka.KafkaProducer;
import com.payserver.repository.OrderRepository;
import com.payserver.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final TossPaymentClient tossPaymentClient;
    private final KafkaProducer kafkaProducer;

    /**
     * 결제 준비 (Order 생성 후 Payment 생성)
     */
    @Transactional
    public Payment createPayment(PaymentRequestDto request) {
        // 1. Order 먼저 생성
        Order order = Order.builder()
                .userId(request.getUserId())
                .orderName(request.getOrderName())
                .totalAmount(request.getAmount())
                .ticketQuantity(request.getTicketQuantity() != null ? request.getTicketQuantity() : 1)
                .orderStatus(OrderStatus.PENDING)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created: orderId={}, userId={}, amount={}",
                savedOrder.getOrderId(), request.getUserId(), request.getAmount());

        // 2. Payment 생성 (Order의 ID 참조)
        Payment payment = Payment.builder()
                .userId(request.getUserId())
                .orderId(savedOrder.getOrderId())
                .orderName(request.getOrderName())
                .amount(request.getAmount())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: paymentId={}, orderId={}, userId={}, amount={}",
                savedPayment.getPaymentId(), savedOrder.getOrderId(), request.getUserId(), request.getAmount());

        return savedPayment;
    }

    /**
     * 결제 승인 (Toss Payment API 호출)
     */
    @Transactional
    public Payment confirmPayment(PaymentConfirmDto confirmDto) {
        try {
            log.info("Confirming payment - orderId: {}, tossOrderId: {}, paymentKey: {}, amount: {}",
                    confirmDto.getOrderId(), confirmDto.getTossOrderId(),
                    confirmDto.getPaymentKey(), confirmDto.getAmount());

            // Toss Payment API 호출 (Toss는 원래 보낸 orderId 형식을 받음)
            TossPaymentConfirmRequest tossRequest = TossPaymentConfirmRequest.builder()
                    .paymentKey(confirmDto.getPaymentKey())
                    .orderId(confirmDto.getTossOrderId())  // Toss에 보낸 원본 orderId 사용
                    .amount(confirmDto.getAmount())
                    .build();

            log.info("Sending to Toss API - request: paymentKey={}, orderId={}, amount={}",
                    tossRequest.getPaymentKey(), tossRequest.getOrderId(), tossRequest.getAmount());

            TossPaymentResponse tossResponse = tossPaymentClient.confirmPayment(tossRequest);

            // DB 업데이트
            Payment payment = paymentRepository.findByOrderId(confirmDto.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Payment not found: " + confirmDto.getOrderId()));

            payment.setPaymentKey(tossResponse.getPaymentKey());
            payment.setPaymentMethod(tossResponse.getMethod());
            payment.setPaymentStatus(PaymentStatus.COMPLETED);

            Payment updatedPayment = paymentRepository.save(payment);

            // Order 상태 업데이트
            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found: " + payment.getOrderId()));
            order.setOrderStatus(OrderStatus.PAID);
            orderRepository.save(order);

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

            // Order 상태도 업데이트
            Order order = orderRepository.findById(payment.getOrderId())
                    .orElseThrow(() -> new RuntimeException("Order not found"));
            order.setOrderStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);

            throw new RuntimeException("Payment confirmation failed", e);
        }
    }

    /**
     * 결제 조회 (orderId로)
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderId(Long orderId) {
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
