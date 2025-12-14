# ADR 009: Ingress Strategy with Lambda Proxy / Estrategia de Ingress con Lambda Proxy

**Date/Fecha:** 2025-12-11
**Status:** Superseded by ADR 011 / Reemplazado por ADR 011
**Supersedes:** ADR 006

## Context / Contexto
We initially attempted to use **AWS API Gateway** and later a **"Smart Proxy" Lambda** to manage traffic and authentication. While the Lambda solved the instability of simulating API Gateway in LocalStack, it introduced imperative maintenance overhead (Node.js code) and an extra network hop.
Inicialmente intentamos usar **AWS API Gateway** y luego una **Lambda "Smart Proxy"** para gestionar tráfico y autenticación. Aunque la Lambda resolvió la inestabilidad de simular API Gateway en LocalStack, introdujo una sobrecarga de mantenimiento imperativo (código Node.js) y un salto de red extra.

## Decision / Decisión
**[SUPERSEDED / REEMPLAZADO]**

We will discontinue the **Lambda Proxy** pattern.
Descontinuaremos el patrón de **Lambda Proxy**.

Traffic Routing and Ingress management will now be handled natively by **Istio Ingress Gateway** (Standard K8s LoadBalancer).
El Enrutamiento de Tráfico y la gestión de Ingress ahora serán manejados nativamente por **Istio Ingress Gateway** (LoadBalancer estándar de K8s).

## Rationale / Justificación
* **Latency:** The Lambda added distinct network latency (`Internet -> Lambda -> K8s`). Istio allows direct entry (`Internet -> K8s`). / La Lambda agregaba latencia de red (`Internet -> Lambda -> K8s`). Istio permite entrada directa (`Internet -> K8s`).
* **Maintenance:** Maintaining custom Node.js proxy logic duplicates features already present in Envoy/Istio. / Mantener lógica de proxy personalizada en Node.js duplica funcionalidades ya presentes en Envoy/Istio.
* **Standardization:** Using standard Kubernetes `Gateway` and `VirtualService` resources allows for GitOps and easier portability. / Usar recursos estándar `Gateway` y `VirtualService` de Kubernetes permite GitOps y mayor portabilidad.

## Consequences / Consecuencias
* **Positive:** Reduced infrastructure code (Terraform). Lower latency. / Reducción de código de infraestructura (Terraform). Menor latencia.
* **Negative:** Requires exposing the JWKS endpoint of the Auth Service to the mesh. / Requiere exponer el endpoint JWKS del Auth Service a la malla.