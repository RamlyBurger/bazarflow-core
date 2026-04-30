# ADR 0002: Use Spring Boot 3.5 First

## Status

Accepted.

## Context

Spring Boot 4 is current, but it is a major migration line. The first implementation should maximize compatibility with Springdoc, Keycloak, Spring Modulith 1.4, Testcontainers, and common enterprise integrations.

## Decision

Start with a Java 17+ baseline, verify on Java 21 LTS, and use Spring Boot 3.5.14 with Spring Modulith 1.4.11 internally. Create a separate upgrade branch for Spring Boot 4 and Spring Modulith 2.

## Consequences

- The first release can move faster on a stable ecosystem baseline.
- The application remains compatible with Java 17+ environments while still being tested on Java 21 LTS.
- The upgrade story is explicit instead of accidental.
