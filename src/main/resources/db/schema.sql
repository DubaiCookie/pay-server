-- ============================================
-- Pay Server Database Schema
-- ============================================

-- 기존 테이블 삭제 (개발 환경에서만 사용, 운영에서는 주석 처리)
-- DROP TABLE IF EXISTS refunds;
-- DROP TABLE IF EXISTS payments;

-- ============================================
-- 1. payments 테이블 (결제 정보)
-- ============================================
CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '결제 ID (PK)',
    user_id BIGINT NOT NULL COMMENT '사용자 ID (FK)',
    order_id VARCHAR(100) NOT NULL UNIQUE COMMENT '주문번호 (UUID)',
    order_name VARCHAR(255) NOT NULL COMMENT '주문명',
    amount BIGINT NOT NULL COMMENT '결제 금액',
    payment_key VARCHAR(200) UNIQUE COMMENT 'Toss 결제 키',
    payment_method VARCHAR(50) COMMENT '결제 수단 (카드, 계좌이체 등)',
    payment_status VARCHAR(20) NOT NULL COMMENT '결제 상태 (PENDING, COMPLETED, FAILED, CANCELLED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME COMMENT '수정 일시',

    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id),
    INDEX idx_payment_key (payment_key),
    INDEX idx_payment_status (payment_status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='결제 정보';

-- ============================================
-- 2. refunds 테이블 (환불 정보)
-- ============================================
CREATE TABLE IF NOT EXISTS refunds (
    refund_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '환불 ID (PK)',
    payment_id BIGINT NOT NULL COMMENT '결제 ID (FK)',
    refund_amount BIGINT NOT NULL COMMENT '환불 금액',
    refund_reason VARCHAR(500) COMMENT '환불 사유',
    refund_status VARCHAR(20) NOT NULL COMMENT '환불 상태 (PENDING, COMPLETED, FAILED)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at DATETIME COMMENT '수정 일시',

    INDEX idx_payment_id (payment_id),
    INDEX idx_refund_status (refund_status),
    INDEX idx_created_at (created_at),

    CONSTRAINT fk_refunds_payment_id
        FOREIGN KEY (payment_id)
        REFERENCES payments(payment_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='환불 정보';

-- ============================================
-- 초기 데이터 (테스트용, 선택사항)
-- ============================================

-- 테스트 결제 데이터
-- INSERT INTO payments (user_id, order_id, order_name, amount, payment_status, created_at) VALUES
-- (1, 'order-uuid-001', '테마파크 입장권', 50000, 'COMPLETED', NOW()),
-- (1, 'order-uuid-002', '놀이기구 이용권', 30000, 'COMPLETED', NOW()),
-- (2, 'order-uuid-003', '프리미엄 티켓', 100000, 'PENDING', NOW());
