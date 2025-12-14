# ADR 002: Adoption of Istio Service Mesh / Adopción de Istio Service Mesh

**Date:** 2025-12-07
**Status:** Accepted

## Context
In a distributed microservices environment, managing traffic, enforcing security (mTLS), and ensuring observability across services becomes complex if handled within the application code.
En un entorno de microservicios distribuidos, gestionar el tráfico, forzar la seguridad (mTLS) y asegurar la observabilidad se vuelve complejo si se maneja en el código de la aplicación.

## Decision
We will use **Istio** with the `demo` profile for the MVP/Dev environment.
Usaremos **Istio** con el perfil `demo` para el entorno MVP/Dev.

## Rationale / Justificación
1.  **Security:** Istio provides transparent mTLS between pods without code changes. / Provee mTLS transparente sin cambiar código.
2.  **Traffic Control:** Enables Canary Deployments and Circuit Breakers. / Permite Despliegues Canary y Circuit Breakers.
3.  **Observability:** Automatically generates metrics for Prometheus/Grafana/Kiali. / Genera métricas automáticas.

## Consequences
* **Negative:** Adds CPU/Memory overhead due to Envoy sidecars. / Agrega consumo de CPU/Memoria por los sidecars.
* **Positive:** Decouples networking logic from business logic. / Desacopla la lógica de red de la de negocio.