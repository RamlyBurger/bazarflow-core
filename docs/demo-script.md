# Demo Script

The target demo flow is:

1. Start Docker Compose and sign in through Keycloak as `ops.manager`.
2. Receive multiple frozen product lots with different expiry dates.
3. Create a retailer order.
4. Apply pricing rules.
5. Reserve stock using FEFO allocation.
6. Accept the order.
7. Plan dispatch.
8. Review SLA and expiry risk in the dashboard.
9. Complete delivery.
10. Verify the audit timeline and hash chain.
