# Pay Server

Toss Payment API를 활용한 결제 및 환불 서비스

## 기술 스택

- **Java 17**
- **Spring Boot 3.2.2**
- **MariaDB** - 데이터베이스
- **Apache Kafka** - auth-server와의 이벤트 기반 통신
- **Toss Payments API** - 결제 및 환불 처리
- **Swagger/OpenAPI** - API 문서화
- **Docker** - 컨테이너화
- **Jenkins** - CI/CD

## 주요 기능

### 1. 결제 기능
- 결제 준비 (orderId 생성)
- 결제 승인 (Toss Payment API 연동)
- 결제 조회
- 사용자별 결제 내역 조회

### 2. 환불 기능
- 결제 취소 및 환불 처리
- 환불 내역 조회

### 3. Kafka 통합
- 결제 완료 이벤트 발행 (payment-events 토픽)
- 환불 완료 이벤트 발행 (refund-events 토픽)
- auth-server로부터 결제/환불 요청 수신 (선택적)

## API 엔드포인트

### 결제 API

- `POST /api/payments` - 결제 준비
- `POST /api/payments/confirm` - 결제 승인
- `GET /api/payments/{paymentId}` - 결제 조회
- `GET /api/payments/order/{orderId}` - 주문번호로 결제 조회
- `GET /api/payments/user/{userId}` - 사용자 결제 내역 조회

### 환불 API

- `POST /api/refunds` - 환불 처리
- `GET /api/refunds/{refundId}` - 환불 조회
- `GET /api/refunds/payment/{paymentId}` - 결제별 환불 내역 조회

### 기타

- `GET /api/health` - 헬스 체크

## Swagger UI

서버 실행 후 다음 URL에서 API 문서 확인:
```
http://localhost:8081/swagger-ui.html
```

## 설정

### application.properties

```properties
# 서버 포트
server.port=8081

# 데이터베이스 설정
spring.datasource.url=jdbc:mariadb://localhost:3379/sql_db
spring.datasource.username=root
spring.datasource.password=SqlDba-1

# Kafka 설정
spring.kafka.bootstrap-servers=localhost:9092

# Toss Payment API 설정
toss.payment.api.url=https://api.tosspayments.com/v1
toss.payment.api.secret-key=test_sk_YOUR_SECRET_KEY_HERE
toss.payment.api.client-key=test_ck_YOUR_CLIENT_KEY_HERE
```

### Toss Payment 키 설명

**🔑 두 가지 키가 필요합니다:**

1. **Secret Key (시크릿 키)** ← **pay-server에서 사용**
   - 위치: `application.properties`
   - 용도: 결제 승인, 취소, 조회 API 호출
   - 보안: ⚠️ 절대 노출 금지 (백엔드에서만 사용)

2. **Client Key (클라이언트 키)** ← **front-end에서 사용**
   - 위치: 프론트엔드 코드
   - 용도: Toss 결제창 호출
   - 보안: 공개되어도 안전 (읽기 전용)

**상세한 결제 플로우는 `docs/PAYMENT_FLOW.md` 참고**

## 빌드 및 실행

### 로컬 실행

```bash
# 빌드
./gradlew clean build

# 실행
./gradlew bootRun
```

### Docker 빌드

```bash
# JAR 파일 생성
./gradlew clean build

# Docker 이미지 빌드
docker build -t pay-server .

# Docker 실행
docker run -p 8081:8081 pay-server
```

## 데이터베이스 스키마

### payments 테이블
- payment_id (PK)
- user_id
- order_id (unique)
- order_name
- amount
- payment_key
- payment_method
- payment_status (PENDING, COMPLETED, FAILED, CANCELLED)
- created_at
- updated_at

### refunds 테이블
- refund_id (PK)
- payment_id
- refund_amount
- refund_reason
- refund_status (PENDING, COMPLETED, FAILED)
- created_at
- updated_at

## Kafka 토픽

### Producer (발행)
- `payment-events` - 결제 완료 이벤트
- `refund-events` - 환불 완료 이벤트

### Consumer (구독)
- `payment-requests` - 결제 요청 (선택적)
- `refund-requests` - 환불 요청 (선택적)

## CI/CD

Jenkins를 통한 자동 배포:
1. Git 소스코드 체크아웃
2. Gradle 빌드
3. Docker 이미지 빌드
4. Docker Hub Push
5. 원격 서버 배포

## 참고 자료

- [Toss Payments API 문서](https://docs.tosspayments.com/)
- [Spring Kafka 문서](https://docs.spring.io/spring-kafka/reference/html/)
- [Spring Boot 문서](https://docs.spring.io/spring-boot/docs/current/reference/html/)
