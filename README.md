# BazarFlow Core

BazarFlow Core is an internal SaaS MVP for chilled and frozen food distributors. The practical business problem is simple: many small distributors still coordinate orders, stock, expiry dates, pricing, and delivery through Excel sheets, WhatsApp messages, and manual checks. Once SKUs, batches, outlets, and delivery windows grow, that workflow becomes hard to audit and easy to break.

This project turns that realistic operations problem into a production-style Java system. It demonstrates Java 21, Spring Boot, Spring Modulith, PostgreSQL, OAuth2 security, idempotent order submission, expiry-aware inventory reservation, event-driven workflows, and a React operations dashboard.

面试介绍时可以这样说：

这个项目可以理解成一个冷冻食品批发业务的内部 SaaS MVP。批发团队原本用 Excel 和人工方式处理订单、库存、批次过期和配送安排，我把这些真实痛点抽象成一个完整的系统来做。它不是普通 CRUD，而是重点展示订单、库存、定价、配送、审计和 Dashboard 这些业务链路。

技术上，我也把它当成系统化练习 Java 21 和 Spring Modulith 架构的实战项目：后端主攻模块边界、库存预留、订单幂等、审计链和安全认证；前端用 React 做一个运营人员可以直接看懂的 Dashboard。

## Current Status

The GitHub repository is intentionally empty for now. Implementation files should be added in small, verified Git-backed steps during Phase 0 and later phases.
