# ADR 003: Hexagonal Architecture (Ports & Adapters) / Arquitectura Hexagonal

**Date:** 2025-12-07
**Status:** Accepted

## Context
Banking logic ("Core Domain") must remain isolated from external frameworks, databases, and UI to ensure long-term maintainability and testability.
La lógica bancaria ("Dominio Core") debe permanecer aislada de frameworks externos, bases de datos e interfaces para asegurar mantenibilidad y testabilidad.

## Decision
All microservices will follow **Hexagonal Architecture**.
Todos los microservicios seguirán **Arquitectura Hexagonal**.
* **Domain:** POJOs only. No Spring/JPA annotations.
* **Ports:** Interfaces defining inputs/outputs.
* **Adapters:** Implementation of interfaces (e.g., RestController, JpaRepository).

## Consequences
* **Positive:** We can switch databases or frameworks without touching the core banking logic. High testability (Unit Tests).
* **Positivo:** Podemos cambiar BDs o frameworks sin tocar la lógica bancaria. Alta testabilidad.
* **Negative:** More boilerplate code (mapping between DTOs and Domain Entities).
* **Negativo:** Más código repetitivo (mapeo entre DTOs y Entidades de Dominio).