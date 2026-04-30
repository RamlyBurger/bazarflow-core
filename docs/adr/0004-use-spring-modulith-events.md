# ADR 0004: Use Internal Domain Events

## Status

Accepted.

## Context

Ordering, inventory, fulfillment, audit, and reporting need to react to workflow changes without direct service coupling.

## Decision

Use Spring application events with Spring Modulith publication support internally first. Externalize selected events to Kafka after core workflows are implemented.

## Consequences

- Module interactions stay explicit and testable.
- Kafka is introduced after business behavior works locally.
- Event publication can be verified with module tests.
