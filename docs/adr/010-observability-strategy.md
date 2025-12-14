# ADR 010: Observability Strategy (Tracing & Health) / Estrategia de Observabilidad

**Date:** 2025-12-14
**Status:** Accepted

## Context
In a distributed microservices environment (Core Banking calling Credit Risk via gRPC), a failure in a downstream service returns a generic error to the user. Developers cannot easily correlate logs between services to find the root cause. Additionally, Kubernetes needs a standardized way to know if a service is alive or ready to accept traffic.

En un entorno de microservicios distribuidos (Core Banking llamando a Credit Risk vía gRPC), un fallo en un servicio downstream devuelve un error genérico al usuario. Los desarrolladores no pueden correlacionar fácilmente los logs entre servicios para encontrar la causa raíz. Además, Kubernetes necesita una forma estandarizada de saber si un servicio está vivo o listo para aceptar tráfico.

## Decision
1.  **Distributed Tracing:** We will implement **Correlation IDs** (Trace IDs) using **MDC (Mapped Diagnostic Context)** and **gRPC Metadata**.
    * **Core Banking (Client):** Generates a `trace-id` (if missing) and sends it in the gRPC Header.
    * **Credit Risk (Server):** Extracts the `trace-id` from the header and injects it into its own Logging Context (MDC).
    * **Result:** All logs for a single transaction will share the same ID across services.

2.  **Health Checks:** We will use **Spring Boot Actuator** to expose standard health endpoints (`/actuator/health`).
    * **HTTP Probe:** K8s Liveness/Readiness probes will hit the HTTP endpoint.
    * **gRPC Health:** The gRPC server will expose the standard `grpc.health.v1.Health` service for internal mesh checks.

3.  **Async Propagation:** For event-driven communication (SQS), the **Trace ID** will be injected as a standard **SQS Message Attribute** (`traceId`) by the publisher and restored into the MDC by the listener.

## Rationale / Justificación
* **Debugging:** "Follow the thread" debugging is impossible without a shared ID across service boundaries. / La depuración "siguiendo el hilo" es imposible sin un ID compartido a través de las fronteras de los servicios.
* **Resilience:** Kubernetes needs accurate health signals to restart hung pods or route traffic away from unready services. / Kubernetes necesita señales de salud precisas para reiniciar pods colgados o desviar tráfico de servicios no preparados.
* **Full Context:** Ensures that logs from the *initiation* of a transfer (REST) to the *final audit* (SQS worker) can be correlated by a single ID. / Asegura que los logs desde el *inicio* de una transferencia (REST) hasta la *auditoría final* (worker SQS) puedan ser correlacionados por un único ID.

## Consequences
* **Implemented:** All services must include `spring-boot-starter-actuator`.
* **Standard:** gRPC Interceptors must be registered in both Client (Core) and Server (Risk) to handle metadata propagation automatically.