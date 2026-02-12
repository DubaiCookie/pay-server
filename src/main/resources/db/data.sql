-- ============================================
-- Pay Server Test Data
-- ============================================

-- 테스트 결제 데이터 삽입
-- 주의: user_id는 auth-server의 users 테이블에 존재해야 함

INSERT INTO payments (user_id, order_id, order_name, amount, payment_key, payment_method, payment_status, created_at) VALUES
(1, 'test-order-001', '테마파크 일반 입장권', 50000, 'test-payment-key-001', '카드', 'COMPLETED', NOW()),
(1, 'test-order-002', '놀이기구 프리미엄 패스', 30000, 'test-payment-key-002', '카드', 'COMPLETED', NOW()),
(2, 'test-order-003', 'VIP 입장권', 100000, 'test-payment-key-003', '간편결제', 'COMPLETED', NOW()),
(2, 'test-order-004', '패밀리 패키지', 150000, NULL, NULL, 'PENDING', NOW()),
(3, 'test-order-005', '단체 할인 티켓', 80000, 'test-payment-key-005', '카드', 'FAILED', NOW());

-- 테스트 환불 데이터 삽입
INSERT INTO refunds (payment_id, refund_amount, refund_reason, refund_status, created_at) VALUES
(1, 50000, '일정 변경으로 인한 환불 요청', 'COMPLETED', NOW()),
(2, 15000, '부분 환불 (일부 이용권 미사용)', 'COMPLETED', NOW()),
(3, 100000, '고객 단순 변심', 'PENDING', NOW());
