# Pay Server Database 설계 문서

## 📊 ERD (Entity Relationship Diagram)

```
┌─────────────────────────────────────────┐
│              payments                    │
├─────────────────────────────────────────┤
│ PK │ payment_id         BIGINT          │
│    │ user_id            BIGINT          │
│    │ order_id           VARCHAR(100)    │
│    │ order_name         VARCHAR(255)    │
│    │ amount             BIGINT          │
│    │ payment_key        VARCHAR(200)    │
│    │ payment_method     VARCHAR(50)     │
│    │ payment_status     VARCHAR(20)     │
│    │ created_at         DATETIME        │
│    │ updated_at         DATETIME        │
└────┬────────────────────────────────────┘
     │
     │ 1:N
     │
     ▼
┌─────────────────────────────────────────┐
│              refunds                     │
├─────────────────────────────────────────┤
│ PK │ refund_id          BIGINT          │
│ FK │ payment_id         BIGINT          │
│    │ refund_amount      BIGINT          │
│    │ refund_reason      VARCHAR(500)    │
│    │ refund_status      VARCHAR(20)     │
│    │ created_at         DATETIME        │
│    │ updated_at         DATETIME        │
└─────────────────────────────────────────┘
```

## 🗂️ 테이블 상세 설명

### 1. payments (결제 정보)

결제 요청 및 완료된 결제 내역을 저장하는 테이블

| 컬럼명 | 타입 | NULL | 키 | 설명 |
|--------|------|------|-----|------|
| payment_id | BIGINT | NOT NULL | PK | 결제 ID (자동 증가) |
| user_id | BIGINT | NOT NULL | IDX | 사용자 ID (auth-server의 users.id) |
| order_id | VARCHAR(100) | NOT NULL | UK | 주문번호 (UUID, 중복 불가) |
| order_name | VARCHAR(255) | NOT NULL | - | 주문명 (상품명) |
| amount | BIGINT | NOT NULL | - | 결제 금액 (원 단위) |
| payment_key | VARCHAR(200) | NULL | UK | Toss 결제 키 (승인 후 생성) |
| payment_method | VARCHAR(50) | NULL | - | 결제 수단 (카드, 계좌이체, 간편결제 등) |
| payment_status | VARCHAR(20) | NOT NULL | IDX | 결제 상태 (아래 참고) |
| created_at | DATETIME | NOT NULL | IDX | 생성 일시 |
| updated_at | DATETIME | NULL | - | 수정 일시 |

#### payment_status (결제 상태)

| 상태 | 설명 | 전환 시점 |
|------|------|----------|
| PENDING | 결제 대기 | 결제 준비 API 호출 시 |
| COMPLETED | 결제 완료 | Toss API 승인 완료 시 |
| FAILED | 결제 실패 | Toss API 승인 실패 시 |
| CANCELLED | 결제 취소 | 환불 완료 시 |

#### 상태 전환 다이어그램

```
PENDING ──승인 성공──> COMPLETED ──환불──> CANCELLED
   │
   └──승인 실패──> FAILED
```

---

### 2. refunds (환불 정보)

결제 취소 및 환불 내역을 저장하는 테이블

| 컬럼명 | 타입 | NULL | 키 | 설명 |
|--------|------|------|-----|------|
| refund_id | BIGINT | NOT NULL | PK | 환불 ID (자동 증가) |
| payment_id | BIGINT | NOT NULL | FK | 결제 ID (payments.payment_id) |
| refund_amount | BIGINT | NOT NULL | - | 환불 금액 (원 단위) |
| refund_reason | VARCHAR(500) | NULL | - | 환불 사유 |
| refund_status | VARCHAR(20) | NOT NULL | IDX | 환불 상태 (아래 참고) |
| created_at | DATETIME | NOT NULL | IDX | 생성 일시 |
| updated_at | DATETIME | NULL | - | 수정 일시 |

#### refund_status (환불 상태)

| 상태 | 설명 | 전환 시점 |
|------|------|----------|
| PENDING | 환불 대기 | 환불 요청 시 |
| COMPLETED | 환불 완료 | Toss API 취소 완료 시 |
| FAILED | 환불 실패 | Toss API 취소 실패 시 |

---

## 🔗 외래 키 (Foreign Key)

```sql
refunds.payment_id → payments.payment_id (CASCADE)
```

- 결제가 삭제되면 연관된 환불 정보도 함께 삭제됩니다.

---

## 📌 인덱스 (Index)

### payments 테이블
- `PRIMARY KEY (payment_id)`
- `UNIQUE KEY (order_id)` - 주문번호 중복 방지
- `UNIQUE KEY (payment_key)` - Toss 결제 키 중복 방지
- `INDEX (user_id)` - 사용자별 결제 조회 최적화
- `INDEX (payment_status)` - 상태별 결제 조회 최적화
- `INDEX (created_at)` - 날짜별 조회 최적화

### refunds 테이블
- `PRIMARY KEY (refund_id)`
- `INDEX (payment_id)` - 결제별 환불 조회 최적화
- `INDEX (refund_status)` - 상태별 환불 조회 최적화
- `INDEX (created_at)` - 날짜별 조회 최적화

---

## 🚀 테이블 생성 방법

### 방법 1: JPA 자동 생성 (기본 설정)

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=update
```

- ✅ 장점: Entity 클래스만 작성하면 자동 생성
- ⚠️ 주의: 운영 환경에서는 `validate`로 변경 권장

---

### 방법 2: SQL 스크립트 직접 실행

```bash
# MariaDB 접속
mysql -h localhost -P 3379 -u root -p sql_db

# SQL 파일 실행
source /home/bae/workspace/miniProject/pay-server/src/main/resources/db/schema.sql
```

또는 Docker 환경:

```bash
# 컨테이너 내부로 들어가기
docker exec -it mariadb mysql -u root -p

# 데이터베이스 선택
USE sql_db;

# 스크립트 실행 (복사-붙여넣기)
```

---

### 방법 3: Spring Boot 자동 실행 (권장)

`application.properties`에 다음 설정 추가:

```properties
# schema.sql 자동 실행 (테이블 생성)
spring.sql.init.mode=always
spring.sql.init.schema-locations=classpath:db/schema.sql

# data.sql 자동 실행 (테스트 데이터)
spring.sql.init.data-locations=classpath:db/data.sql

# JPA ddl-auto 비활성화 (SQL 스크립트 사용 시)
spring.jpa.hibernate.ddl-auto=none
```

---

## 🧪 테스트 데이터

`data.sql` 파일에 테스트 데이터가 포함되어 있습니다:

- ✅ 완료된 결제 3건
- ⏳ 대기 중인 결제 1건
- ❌ 실패한 결제 1건
- ✅ 완료된 환불 2건
- ⏳ 대기 중인 환불 1건

---

## 📊 데이터 예시

### payments 테이블

```sql
SELECT * FROM payments;
```

| payment_id | user_id | order_id | order_name | amount | payment_status |
|------------|---------|----------|------------|--------|----------------|
| 1 | 1 | test-order-001 | 테마파크 일반 입장권 | 50000 | COMPLETED |
| 2 | 1 | test-order-002 | 놀이기구 프리미엄 패스 | 30000 | COMPLETED |
| 3 | 2 | test-order-003 | VIP 입장권 | 100000 | COMPLETED |
| 4 | 2 | test-order-004 | 패밀리 패키지 | 150000 | PENDING |
| 5 | 3 | test-order-005 | 단체 할인 티켓 | 80000 | FAILED |

### refunds 테이블

```sql
SELECT * FROM refunds;
```

| refund_id | payment_id | refund_amount | refund_reason | refund_status |
|-----------|------------|---------------|---------------|---------------|
| 1 | 1 | 50000 | 일정 변경으로 인한 환불 요청 | COMPLETED |
| 2 | 2 | 15000 | 부분 환불 (일부 이용권 미사용) | COMPLETED |
| 3 | 3 | 100000 | 고객 단순 변심 | PENDING |

---

## 🔍 유용한 쿼리

### 사용자별 결제 내역 조회

```sql
SELECT
    p.payment_id,
    p.order_id,
    p.order_name,
    p.amount,
    p.payment_status,
    p.created_at,
    COALESCE(SUM(r.refund_amount), 0) as total_refund
FROM payments p
LEFT JOIN refunds r ON p.payment_id = r.payment_id AND r.refund_status = 'COMPLETED'
WHERE p.user_id = 1
GROUP BY p.payment_id
ORDER BY p.created_at DESC;
```

### 일별 결제 금액 집계

```sql
SELECT
    DATE(created_at) as payment_date,
    COUNT(*) as total_count,
    SUM(CASE WHEN payment_status = 'COMPLETED' THEN amount ELSE 0 END) as total_amount
FROM payments
GROUP BY DATE(created_at)
ORDER BY payment_date DESC;
```

### 환불률 계산

```sql
SELECT
    COUNT(DISTINCT p.payment_id) as total_payments,
    COUNT(DISTINCT r.payment_id) as refunded_payments,
    ROUND(COUNT(DISTINCT r.payment_id) * 100.0 / COUNT(DISTINCT p.payment_id), 2) as refund_rate
FROM payments p
LEFT JOIN refunds r ON p.payment_id = r.payment_id AND r.refund_status = 'COMPLETED'
WHERE p.payment_status = 'COMPLETED';
```

---

## ⚠️ 주의사항

1. **user_id 참조**: auth-server의 `users` 테이블에 존재하는 ID만 사용
2. **amount 단위**: 원 단위로 저장 (소수점 없음)
3. **order_id 중복**: UUID 사용으로 중복 방지
4. **payment_key**: Toss 승인 후에만 값이 들어감 (PENDING 상태에서는 NULL)
5. **환불 금액**: 원본 결제 금액보다 클 수 없음 (애플리케이션 레벨에서 검증 필요)

---

## 🔐 보안 권장사항

1. **최소 권한 원칙**: pay-server 전용 DB 사용자 생성
2. **읽기 전용 복제본**: 조회용 쿼리는 읽기 복제본 사용
3. **민감 정보 암호화**: 필요시 payment_key 등 암호화 저장
4. **감사 로그**: 결제/환불 변경 이력 별도 테이블 관리 (선택)

---

## 📚 참고 자료

- [MariaDB 공식 문서](https://mariadb.com/kb/en/)
- [JPA/Hibernate DDL 설정](https://docs.spring.io/spring-boot/docs/current/reference/html/data.html#data.sql.jpa-and-spring-data)
- [Toss Payments API](https://docs.tosspayments.com/)
