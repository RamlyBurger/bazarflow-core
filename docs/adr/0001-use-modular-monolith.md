# ADR 0001: Use A Modular Monolith First

## Status

Accepted.

## Context

The system needs realistic domain depth across ordering, inventory, pricing, fulfillment, audit, and reporting. Splitting those concerns into services before the core workflow exists would add infrastructure work without improving the first portfolio outcome.

## Decision

Build one Spring Boot application and enforce module boundaries with Spring Modulith.

## Consequences

- Local development stays simple.
- Tests can cover cross-module workflows without network calls.
- Kafka and service extraction remain available later through domain events.
