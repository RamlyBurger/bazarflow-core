# Security

Do not commit secrets, tokens, private keys, production credentials, or real customer data.

Use `.env.example` for documented local defaults. Real local overrides should live in `.env`, which is ignored by Git.

Security-related implementation goals:

- OAuth2 resource server integration with Keycloak
- Role-based authorization at method boundaries
- Idempotency for mutating order commands
- Tamper-evident operational audit chain
- Correlation IDs for request tracing
