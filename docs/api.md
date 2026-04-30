# API Notes

## Public Endpoints

```http
GET /api/platform/status
GET /actuator/health
GET /swagger-ui.html
```

## Partner Endpoints

Protected partner endpoints currently use Spring Security roles. Local tests use mock users; a full identity provider configuration will be wired in a later security slice.

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
```

Role access:

| Endpoint | Roles |
|---|---|
| `POST /api/inventory/lots` | `OPS_MANAGER`, `WAREHOUSE` |
| `GET /api/inventory/lots` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |
| `GET /api/inventory/availability` | `OPS_MANAGER`, `WAREHOUSE`, `SALES`, `AUDITOR` |

Receiving a lot creates an available inventory lot and records an initial `RECEIVE` stock movement. Quantity constraints are enforced in both API validation and PostgreSQL checks.

The API echoes `X-Correlation-Id` when provided and generates one when it is missing. Validation and business errors return RFC 9457-style problem details with an `errorCode` property.
