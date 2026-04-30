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

The API echoes `X-Correlation-Id` when provided and generates one when it is missing. Validation and business errors return RFC 9457-style problem details with an `errorCode` property.
