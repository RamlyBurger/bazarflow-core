# ADR 0003: Use PostgreSQL Schema Per Module

## Status

Accepted.

## Context

The application starts as a single deployable unit with one database. It still needs clear data ownership by bounded context.

## Decision

Use one PostgreSQL database and create one schema per business module.

## Consequences

- Module ownership is visible in the database.
- Cross-module joins remain possible for reporting.
- Future extraction can move one schema at a time.
