# ADR 001: Hybrid Cloud Infrastructure Strategy / Estrategia de Infraestructura Híbrida

**Date/Fecha:** 2025-12-07
**Status:** Accepted / Aceptado

## Context / Contexto
We need a development environment that simulates a Tier-3 Banking Architecture without incurring AWS Cloud costs.
Necesitamos un entorno de desarrollo que simule una Arquitectura Bancaria Tier-3 sin incurrir en costos de AWS Cloud.

## Decision / Decisión
We will use **LocalStack** to emulate AWS APIs (S3, Secrets Manager, IAM) and **Kubernetes (Kind)** to emulate the Compute Layer (EKS).
Useremos **LocalStack** para emular APIs de AWS y **Kubernetes (Kind)** para emular la capa de Cómputo.

We will use **Terraform Feature Flags** (`count` logic) to deploy real RDS instances in Production, but containerized PostgreSQL in Local, while keeping the application configuration agnostic (using Secrets Manager for service discovery).
Usaremos **Terraform Feature Flags** para desplegar RDS real en Producción, pero PostgreSQL contenerizado en Local, manteniendo la aplicación agnóstica (usando Secrets Manager para el descubrimiento de servicios).

## Consequences / Consecuencias
* **Positive:** Zero cost for developers. Identical network topology definition.
* **Positivo:** Costo cero para desarrolladores. Definición de topología de red idéntica.
* **Negative:** LocalStack limitation on RDS requires extra Terraform complexity (conditional logic).
* **Negativo:** La limitación de RDS en LocalStack requiere complejidad extra en Terraform (lógica condicional).