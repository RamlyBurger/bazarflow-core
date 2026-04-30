# API Notes

## Public Endpoints

```http
GET /api/platform/status
GET /actuator/health
GET /swagger-ui.html
```

## Authentication

Protected endpoints accept a Keycloak-issued JWT bearer token:

```http
Authorization: Bearer <access-token>
```

Docker Compose imports the local realm from `config/keycloak/bazarflow-realm.json` when Keycloak starts with an empty data store. The API validates tokens from `http://localhost:8081/realms/bazarflow` and maps both realm roles and `bazarflow-api` client roles to Spring Security authorities.

Local users use the password `bazarflow`:

| Username | Role |
|---|---|
| `ops.manager` | `OPS_MANAGER` |
| `warehouse.operator` | `WAREHOUSE` |
| `sales.operator` | `SALES` |
| `dispatch.operator` | `DISPATCH` |
| `audit.viewer` | `AUDITOR` |

Swagger UI is configured for bearer authentication. Use the `bazarflow-ops-console` public client when authorizing through the local realm. The operations console uses the same public client for local authorization-code sign-in.

## Partner Endpoints

Partner endpoints use Spring Security roles. Local tests use mock users; local runtime tokens come from the Keycloak realm imported by Docker Compose.

```http
POST  /api/retailers
GET   /api/retailers
GET   /api/retailers/{retailerId}
POST  /api/retailers/{retailerId}/outlets
PATCH /api/retailers/{retailerId}/credit-status
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/retailers` | `OPS_MANAGER`, `SALES` |
| `GET /api/retailers` | `OPS_MANAGER`, `SALES`, `AUDITOR` |
| `GET /api/retailers/{retailerId}` | `OPS_MANAGER`, `SALES`, `AUDITOR` |
| `POST /api/retailers/{retailerId}/outlets` | `OPS_MANAGER`, `SALES` |
| `PATCH /api/retailers/{retailerId}/credit-status` | `OPS_MANAGER` |

## Catalog Endpoints

```http
POST  /api/products
GET   /api/products
POST  /api/skus
GET   /api/skus
GET   /api/skus/{skuId}
PATCH /api/skus/{skuId}/status
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/products` | `OPS_MANAGER` |
| `GET /api/products` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `POST /api/skus` | `OPS_MANAGER` |
| `GET /api/skus` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `GET /api/skus/{skuId}` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `PATCH /api/skus/{skuId}/status` | `OPS_MANAGER` |

Products use normalized relational fields plus JSONB metadata for flexible attributes. SKU codes and unit-of-measure values are normalized to uppercase.

## Inventory Endpoints

```http
POST /api/inventory/lots
GET  /api/inventory/lots
GET  /api/inventory/availability?skuId={skuId}
GET  /api/inventory/reservations
GET  /api/inventory/reservations?orderId={orderId}
GET  /api/inventory/reservations/{reservationId}
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/inventory/lots` | `OPS_MANAGER`, `WAREHOUSE` |
| `GET /api/inventory/lots` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `GET /api/inventory/availability` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `GET /api/inventory/reservations` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `GET /api/inventory/reservations/{reservationId}` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |

Receiving a lot creates an available inventory lot and records an initial `RECEIVE` stock movement. Submitting an order creates an active reservation, allocates the earliest valid expiry lots first, updates available and reserved quantities, and records `RESERVE` stock movements. Completing a dispatch job consumes the active reservation, moves reserved lot quantity into dispatched quantity, and records `DISPATCH` stock movements. Quantity constraints are enforced in both API validation and PostgreSQL checks.

## Pricing Endpoints

```http
POST /api/pricing/price-books
GET  /api/pricing/price-books
POST /api/pricing/rules
GET  /api/pricing/rules
POST /api/pricing/quote
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/pricing/price-books` | `OPS_MANAGER` |
| `GET /api/pricing/price-books` | `OPS_MANAGER`, `SALES`, `AUDITOR` |
| `POST /api/pricing/rules` | `OPS_MANAGER` |
| `GET /api/pricing/rules` | `OPS_MANAGER`, `SALES`, `AUDITOR` |
| `POST /api/pricing/quote` | `OPS_MANAGER`, `SALES` |

Pricing rules are deterministic. A quote line selects the highest-priority active rule matching SKU, retailer, delivery zone, validity window, and minimum quantity. If priority ties, the more specific rule wins.

## Order Endpoints

```http
POST /api/orders
GET  /api/orders
GET  /api/orders/{orderId}
POST /api/orders/{orderId}/submit
POST /api/orders/{orderId}/accept
GET  /api/orders/{orderId}/timeline
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/orders` | `OPS_MANAGER`, `SALES` |
| `GET /api/orders` | `OPS_MANAGER`, `SALES`, `WAREHOUSE`, `AUDITOR` |
| `GET /api/orders/{orderId}` | `OPS_MANAGER`, `SALES`, `WAREHOUSE`, `AUDITOR` |
| `POST /api/orders/{orderId}/submit` | `OPS_MANAGER`, `SALES` |
| `POST /api/orders/{orderId}/accept` | `OPS_MANAGER` |
| `GET /api/orders/{orderId}/timeline` | `OPS_MANAGER`, `SALES`, `WAREHOUSE`, `AUDITOR` |

Draft creation prices each order line through the pricing module. Submission requires an `Idempotency-Key` header, reserves stock through the inventory module, and records a status timeline entry. Accepted orders become eligible for fulfillment planning. A blocked retailer cannot submit an order. If stock is insufficient, the order remains in draft state and inventory quantities are unchanged.

Submitting an order also writes audit events for draft creation, stock reservation, and submission. Query `GET /api/audit/events?aggregateType=ORDER&aggregateId={orderId}` for an order-level operational timeline.

## Fulfillment Endpoints

```http
POST /api/fulfillment/pick-waves
GET  /api/fulfillment/pick-waves
GET  /api/fulfillment/pick-waves/{pickWaveId}
POST /api/fulfillment/dispatch-jobs/{dispatchJobId}/complete
POST /api/fulfillment/dispatch-jobs/{dispatchJobId}/fail
GET  /api/fulfillment/sla-risk
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/fulfillment/pick-waves` | `OPS_MANAGER`, `DISPATCH` |
| `GET /api/fulfillment/pick-waves` | `OPS_MANAGER`, `DISPATCH`, `WAREHOUSE`, `AUDITOR` |
| `GET /api/fulfillment/pick-waves/{pickWaveId}` | `OPS_MANAGER`, `DISPATCH`, `WAREHOUSE`, `AUDITOR` |
| `POST /api/fulfillment/dispatch-jobs/{dispatchJobId}/complete` | `OPS_MANAGER`, `DISPATCH` |
| `POST /api/fulfillment/dispatch-jobs/{dispatchJobId}/fail` | `OPS_MANAGER`, `DISPATCH` |
| `GET /api/fulfillment/sla-risk` | `OPS_MANAGER`, `DISPATCH`, `WAREHOUSE`, `AUDITOR` |

Pick wave creation selects accepted orders for the requested delivery date and delivery zone that do not already have dispatch jobs. The response includes planned dispatch jobs with outlet delivery windows and SLA-risk flags. SLA risk is raised when a same-day job can no longer satisfy its delivery window after applying the route buffer.

Completing a dispatch job changes the job to `COMPLETED`, consumes the order reservation, moves the order to `DELIVERED`, and writes inventory, ordering, and fulfillment audit events in the same transaction. Failing a dispatch job changes the job to `FAILED`, records a failure reason, moves the order to `DELIVERY_FAILED`, and keeps the reservation active for later operational handling.

## Reporting Endpoints

```http
GET /api/reporting/dashboard
```

Role access:

| Endpoint | Roles |
|---|---|
| `GET /api/reporting/dashboard` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `DISPATCH`, `AUDITOR` |

The dashboard endpoint is a read-only projection over the existing operational schemas. It returns KPI cards, work queue rows, risk watch items, seven-day throughput, dispatch backlog by zone, and recent audit timeline entries. The React operations console uses this endpoint when the API is available and keeps a local fallback for disconnected development.

## Audit Endpoints

```http
GET /api/audit/events
GET /api/audit/events?aggregateType={aggregateType}
GET /api/audit/events?aggregateType={aggregateType}&aggregateId={aggregateId}
```

Role access:

| Endpoint | Roles |
|---|---|
| `GET /api/audit/events` | `OPS_MANAGER`, `AUDITOR` |

Audit events are append-only records with source module, aggregate type, aggregate ID, event type, message, actor, correlation ID, details, and occurrence time. `limit` defaults to `100` and can be set from `1` to `500`.

The API echoes `X-Correlation-Id` when provided and generates one when it is missing. Validation and business errors return RFC 9457-style problem details with an `errorCode` property.
