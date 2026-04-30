# ADR 0002: Use Spring Boot 3.5 First

## Status

Accepted.

## Context

Spring Boot 4 is current, but it is a major migration line. The first implementation should maximize compatibility with Springdoc, Keycloak, Spring Modulith 1.4, Testcontainers, and common enterprise examples.

## Decision

Start with Java 21, Spring Boot 3.5.14, and Spring Modulith 1.4.11. Create a separate upgrade branch for Spring Boot 4 and Spring Modulith 2.

## Consequences

- The MVP can move faster on a stable ecosystem baseline.
- The project still uses a modern LTS JDK.
- The upgrade story is explicit instead of accidental.
