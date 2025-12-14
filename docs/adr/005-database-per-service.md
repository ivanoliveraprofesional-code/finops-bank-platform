# ADR 005: Database per Service Pattern / Patrón de Base de Datos por Servicio

**Date:** 2025-12-07
**Status:** Accepted

## Context
Microservices must be independently deployable and scalable. Sharing a single monolithic database creates tight coupling ("Distributed Monolith").
Los microservicios deben ser desplegables y escalables independientemente. Compartir una BD monolítica crea acoplamiento fuerte.

## Decision
Each microservice owns its private data. Access to other services' data is only permitted via APIs, never direct SQL.
Cada microservicio es dueño de sus datos. El acceso a datos de otros es solo vía API, nunca SQL directo.

* **Auth Service:** PostgreSQL (`finops_auth`)
* **Core Banking:** PostgreSQL (`finops_core_banking`)
* **Audit Service:** DynamoDB (`finops-audit-logs`) - **IMPLEMENTED** (Using AWS SDK Enhanced Client).

## Consequences
* **Positive:** Failure in one DB does not bring down the whole bank. Schema changes are safer.
* **Positivo:** Fallo en una BD no tumba todo el banco. Cambios de esquema más seguros.
* **Negative:** Cross-service queries are harder (requires aggregation pattern) and Distributed Transactions (Saga Pattern) might be needed.
* **Negativo:** Consultas cruzadas más difíciles y posible necesidad de Transacciones Distribuidas (Saga).