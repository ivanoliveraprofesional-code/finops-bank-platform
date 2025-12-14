# ADR 006: API Gateway Implementation Strategy / Estrategia de API Gateway

**Date:** 2025-12-07
**Status:** Accepted

## Context
We need a unified entry point to manage traffic, enforce rate limiting, and offload security concerns before traffic hits the Kubernetes cluster.
Necesitamos un punto de entrada unificado para gestionar tráfico, límites de tasa y delegar seguridad antes de que el tráfico toque el cluster de Kubernetes.

## Decision
We will use **AWS API Gateway REST API (v1)**.
Usaremos **AWS API Gateway REST API (v1)**.

## Update (2025-12-11)
**This decision has been revoked.** Emulating the complex permission model of API Gateway Authorizers in LocalStack proved unreliable (recurring `API_CONFIGURATION_ERROR`). We have moved to a code-first approach using a Lambda Proxy (See ADR 009).

**Esta decisión ha sido revocada.** La emulación del modelo de permisos complejos de API Gateway en LocalStack resultó poco confiable. Nos hemos movido a un enfoque basado en código usando un Lambda Proxy (Ver ADR 009).
...

## Rationale
* **Tooling Constraint:** LocalStack Community Edition does not support HTTP APIs (v2). REST API (v1) provides full compatibility for local simulation.
* **Feature Set:** REST APIs offer robust support for Lambda Authorizers and Usage Plans (Throttling), essential for a banking simulation.

## Consequences
* **Positive:** Full simulation capability without license costs.
* **Negative:** Terraform configuration is more verbose than v2.