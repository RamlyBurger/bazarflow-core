# ADR 0004: Use Spring Modulith Events

## Status

Accepted.

## Context

Ordering, inventory, fulfillment, audit, and reporting need to react to workflow changes without direct service coupling.

## Decision

Use Spring application events and Spring Modulith publication support for internal events first. Externalize selected events to Kafka after core workflows are implemented.

## Consequences

- Module interactions stay explicit and testable.
- Kafka is introduced after business behavior works locally.
- Event publication can be verified with module tests.
