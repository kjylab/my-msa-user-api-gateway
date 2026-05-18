# my-msa-user-api-gateway

Troica Market MSA의 **사용자 대면 REST API 게이트웨이**. 클라이언트의 HTTP 요청을 받아 내부 마이크로서비스(product/order/inventory)에 gRPC 호출로 변환한다.

## 역할

```
Client (HTTP/REST)
    ↓
user-api-gateway (port 8100)
    ├─ gRPC → product-service   (port 9001)
    ├─ gRPC → order-service     (port 9002)
    └─ gRPC → inventory-service (port 9003)
```

gRPC 클라이언트(`grpc-client-spring-boot-starter`)가 각 서비스의 stub을 주입받아 호출한다.

## REST API 엔드포인트

Swagger UI: `http://<host>/swagger-ui.html`

| 도메인 | 컨트롤러 | 주요 엔드포인트 |
|--------|---------|----------------|
| Product | `UserProductApiGatewayRestController` | `GET /products`, `GET /products/{id}` |
| Order | `UserOrderApiGatewayRestController` | `GET /orders`, `POST /orders` |
| Inventory | `UserInventoryApiGatewayRestController` | `GET /inventories`, `GET /inventories/{id}` |

## 아키텍처

```
adapter/presentation/web/inbound/   ← REST 컨트롤러 (요청 수신)
    product/ order/ inventory/
    configuration/
        SecurityConfig              ← JWT 인증 필터
        GlobalExceptionHandler      ← gRPC StatusException → HTTP 에러 응답 변환

application/
    product/service/ProductQueryService  ← gRPC stub 호출 + 응답 변환
    order/service/OrderQueryService, OrderCommandService
    inventory/service/InventoryQueryService

UserProductApiGatewayRestControllerAdapter   ← 컨트롤러 → 서비스 연결
```

## 예외 처리

gRPC 호출이 실패하면 `StatusRuntimeException`이 발생한다. `GlobalExceptionHandler`가 이를 잡아 gRPC status를 HTTP 상태코드로 매핑해서 반환한다.

```
gRPC Status.NOT_FOUND → HTTP 404
gRPC Status.INVALID_ARGUMENT → HTTP 400
gRPC Status.ALREADY_EXISTS → HTTP 409
gRPC Status.INTERNAL → HTTP 500
```

## 보안

`SecurityConfig`에서 JWT 토큰 검증. `jwt.secret`은 application.yaml에 설정 (운영 시 외부 주입 필요).

## 실행 포트

| 포트 | 용도 |
|------|------|
| 8100 | HTTP REST API |

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
| [my-msa-product](https://github.com/kjylab/my-msa-product) | product-service (gRPC 서버) |
| [my-msa-inventory](https://github.com/kjylab/my-msa-inventory) | inventory-service (gRPC 서버) |
| [my-msa-manifest-values](https://github.com/kjylab/my-msa-manifest-values) | Helm values (이미지 태그 관리) |
