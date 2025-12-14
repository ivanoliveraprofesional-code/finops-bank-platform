# ADR 004: Synchronous Communication Protocol / Protocolo de Comunicación Síncrona

**Date:** 2025-12-07
**Status:** Accepted / Aceptado

## Context
High-volume internal communication between *Auth Service* and *Core Banking* requires low latency. JSON/REST over HTTP/1.1 creates overhead.
La comunicación interna de alto volumen entre servicios requiere baja latencia. JSON/REST sobre HTTP/1.1 genera sobrecarga.

## Decision
We will use **gRPC (Protobuf)** for internal service-to-service communication and **REST** for public-facing APIs.
Usaremos **gRPC (Protobuf)** para comunicación interna entre servicios y **REST** para APIs públicas.

## Rationale
* **Performance:** Binary serialization (Protobuf) is significantly smaller and faster than JSON. / Serialización binaria más rápida y ligera.
* **Contract First:** `.proto` files enforce strict contracts between teams. / Archivos `.proto` fuerzan contratos estrictos.

## Consequences
* Requires defining `.proto` files and generating Java stubs during the build process.
* **Observability:** Implementations must propagate **Correlation IDs** via gRPC Metadata to ensure traceability across the binary protocol.