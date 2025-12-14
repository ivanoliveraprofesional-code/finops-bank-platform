# ADR 007: Edge Security with Lambda Authorizers / Seguridad Perimetral con Lambda Authorizers

**Date/Fecha:** 2025-12-07
**Status:** Superseded by ADR 011 / Reemplazado por ADR 011

## Context / Contexto
Exposing microservices directly to the internet increases the attack surface. Authentication validation consumes resources inside the cluster (Compute Grid).
Exponer microservicios directamente a internet aumenta la superficie de ataque. La validación de autenticación consume recursos dentro del cluster.

## Decision / Decisión
**[SUPERSEDED / REEMPLAZADO]**

We originally implemented a **Split-Ingress Pattern** using a custom **Lambda Authorizer** (or Proxy) to intercept requests before they reached Kubernetes.
Originalmente implementamos un **Patrón de Ingress Dividido** usando un **Lambda Authorizer** (o Proxy) personalizado para interceptar peticiones antes de que llegaran a Kubernetes.

**Update (2025-12-14):**
This imperative approach (custom code) has been replaced by a **Declarative Service Mesh Policy** using **Istio**. See **ADR 011**.
Este enfoque imperativo (código personalizado) ha sido reemplazado por una **Política Declarativa de Service Mesh** usando **Istio**. Ver **ADR 011**.

## Rationale / Justificación
* **Original:** Zero Trust requests were rejected at the edge to save K8s compute cycles. / Las peticiones Zero Trust se rechazaban en el borde para ahorrar ciclos de cómputo de K8s.
* **Update:** Moving this logic to Istio (Envoy Proxy) is more performant (C++ vs Node.js) and adheres to "Configuration as Code" principles. / Mover esta lógica a Istio (Envoy Proxy) es más eficiente (C++ vs Node.js) y se adhiere a los principios de "Configuración como Código".

## Consequences / Consecuencias
* The Lambda Function and its Terraform resources are deprecated and will be removed.
* La Función Lambda y sus recursos de Terraform están obsoletos y serán eliminados.