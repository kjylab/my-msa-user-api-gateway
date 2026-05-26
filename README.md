# my-msa-user-api-gateway

Troica Market MSA의 **사용자 대면 REST API 게이트웨이**. 클라이언트의 HTTP 요청을 받아 JWT 인증 후, 내부 마이크로서비스에 gRPC 또는 HTTP로 라우팅한다.

## 역할

```
Client (HTTP/REST)
    ↓
user-api-gateway (port 8080)
    ├─ HTTP → auth-service   (Spring Cloud Gateway 라우팅)
    ├─ HTTP → user-service   (Spring Cloud Gateway 라우팅)
    ├─ gRPC → product-service
    ├─ gRPC → order-service
    └─ gRPC → inventory-service
```

## 라우팅 구성

| 경로 | 대상 | 인증 |
|------|------|------|
| `/api/auth/**` | auth-service:8080 | 불필요 (PERMIT) |
| `/api/v1/users/**` | user-service:8080 | JWT 필요 |
| `/products/**`, `/orders/**`, `/inventories/**` | gRPC 서비스 | JWT 필요 |

## 아키텍처

```
adapter/presentation/web/inbound/   ← REST 컨트롤러 (gRPC 서비스용)
    product/ order/ inventory/
    configuration/
        SecurityConfig              ← JWT 인증 필터 (JwtAuthFilter)
        GlobalExceptionHandler      ← gRPC StatusException → HTTP 에러 응답 변환

application/
    product/service/ProductQueryService   ← gRPC stub 호출 + 응답 변환
    order/service/OrderQueryService, OrderCommandService
    inventory/service/InventoryQueryService
```

## 예외 처리

```
gRPC Status.NOT_FOUND       → HTTP 404
gRPC Status.INVALID_ARGUMENT → HTTP 400
gRPC Status.ALREADY_EXISTS  → HTTP 409
gRPC Status.INTERNAL        → HTTP 500
CallNotPermittedException   → HTTP 503 (Circuit Breaker OPEN)
```

## Circuit Breaker (Resilience4j)

gRPC 서비스 호출에 Circuit Breaker 적용. 장애 서비스로의 요청을 차단해 연쇄 장애를 방지한다.

| CB 이름 | 대상 |
|---------|------|
| `product-cb` | ProductQueryService |
| `inventory-cb` | InventoryQueryService |
| `order-cb` | OrderCommandService, OrderQueryService |

**CB 설정:**
- sliding-window: 10건
- 실패율 임계치: 50%
- OPEN 후 대기: 60초

**상태 확인:**
```bash
GET /circuitbreakers
GET /circuitbreakerevents
```

## 보안

`JwtAuthFilter`가 요청마다 JWT 토큰을 검증. `PERMIT_PATHS`에 등록된 경로는 인증 없이 통과.

```
PERMIT_PATHS: /api/auth/**, /healthz, /prometheus, /circuitbreakers, /circuitbreakerevents
```

## 실행 포트

| 포트 | 용도 |
|------|------|
| 8080 | HTTP REST API |

## 관측성 (Observability)

### 분산 트레이싱 (Tempo)
- `micrometer-tracing-bridge-otel` + `opentelemetry-exporter-otlp` 사용
- `Hooks.enableAutomaticContextPropagation()` — Reactor WebFlux ↔ ThreadLocal 브릿지
- `TracingGrpcClientInterceptor` (`@GrpcGlobalClientInterceptor`) — gRPC 호출 시 trace context 전파
- Gateway → gRPC 서비스 간 동일 traceId 전파 완료

### 메트릭 (Prometheus)
- `/prometheus` 엔드포인트로 메트릭 노출
- HTTP 요청별 latency histogram bucket 활성화

## CI/CD 흐름

```
GitHub push
  → JAR 빌드
  → Docker 이미지 빌드 + Docker Hub push (jyupk/my-msa-user-api-gateway)
  → my-msa-manifest-values/user-api-gateway/values-release.yaml 의 tag를 커밋 SHA로 업데이트
  → ArgoCD 감지 → 클러스터 롤링 업데이트
```

## 로컬 Docker 빌드

```bash
docker build --no-cache -t ktcloud-msa-user-api-gateway:latest -f Containerfile .
```

## 관련 레포

| 레포 | 역할 |
|------|------|
| [my-msa-product](https://github.com/kjylab/my-msa-product) | product-service |
| [my-msa-inventory](https://github.com/kjylab/my-msa-inventory) | inventory-service |
| [my-msa-order](https://github.com/kjylab/my-msa-order) | order-service |
| [my-msa-auth-service](https://github.com/kjylab/my-msa-auth-service) | auth-service |
| [my-msa-user-service](https://github.com/kjylab/my-msa-user-service) | user-service |
| [my-msa-manifest-values](https://github.com/kjylab/my-msa-manifest-values) | Helm values (이미지 태그 관리) |
