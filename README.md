# BazarFlow Core

BazarFlow Core is an internal SaaS MVP for chilled and frozen food distributors. The practical business problem is simple: many small distributors still coordinate orders, stock, expiry dates, pricing, and delivery through Excel sheets, WhatsApp messages, and manual checks. Once SKUs, batches, outlets, and delivery windows grow, that workflow becomes hard to audit and easy to break.

This project turns that realistic operations problem into a production-style Java system. It demonstrates Java 21, Spring Boot, Spring Modulith, PostgreSQL, OAuth2 security, idempotent order submission, expiry-aware inventory reservation, event-driven workflows, and a React operations dashboard.

Interview positioning: this can be explained as an MVP for a chilled-food wholesale operation that had outgrown spreadsheets. The backend focuses on the business-critical paths: order intake, inventory reservation, batch expiry, pricing, delivery planning, auditability, and operations reporting.

Technically, it is also a focused Java 21 and Spring Modulith practice project. The main backend emphasis is module boundaries, idempotent order commands, inventory consistency, security, audit integrity, and testable domain logic. The frontend is an operations dashboard designed for staff who need to scan risk and act quickly.

## Current Status

The GitHub repository is intentionally empty for now. Implementation files should be added in small, verified Git-backed steps during Phase 0 and later phases.
