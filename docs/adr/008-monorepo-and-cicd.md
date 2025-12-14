# ADR 008: Adoption of Monorepo Structure & Path-Based CI/CD / Adopción de Monorepo y CI/CD Basado en Rutas

**Date/Fecha:** 2025-12-08
**Status:** Accepted / Aceptado

## Context / Contexto
The platform consists of multiple distinct components: Infrastructure (Terraform), multiple Java Microservices (Auth, Core, Audit), Python Serverless functions (Lambda Authorizer), and Documentation.
La plataforma consta de múltiples componentes distintos: Infraestructura (Terraform), múltiples microservicios Java (Auth, Core, Audit), funciones Serverless Python (Lambda Authorizer) y Documentación.

We need to decide how to organize these components in Version Control (Git) to maximize developer velocity, visibility for stakeholders, and deployment reliability.
Necesitamos decidir cómo organizar estos componentes en Control de Versiones (Git) para maximizar la velocidad de desarrollo, la visibilidad para los interesados y la fiabilidad del despliegue.

## Decision / Decisión
We will adopt a **Structured Monorepo** approach. All components will reside in a single GitHub repository, organized by architectural layers (`backend/`, `terraform/`, `docs/`, `scripts/`).
Adoptaremos un enfoque de **Monorepo Estructurado**. Todos los componentes residirán en un único repositorio de GitHub, organizados por capas arquitectónicas.

To handle deployments, we will implement **Path-Filtering CI/CD Pipelines** using GitHub Actions.
Para manejar los despliegues, implementaremos **Pipelines de CI/CD con Filtrado de Rutas** usando GitHub Actions.

## Detailed Rationale / Justificación Detallada

### 1. Why Monorepo? / ¿Por qué Monorepo?
* **Atomic Commits (Cambios Atómicos):** A feature often requires changes across layers (e.g., adding an SQS queue in Terraform AND the Java listener code). A monorepo allows this in a single Pull Request, ensuring infrastructure and code never drift. / Una funcionalidad a menudo requiere cambios en varias capas. Un monorepo permite esto en un solo PR, asegurando que infra y código nunca se desincronicen.
* **Holistic Visibility (Visibilidad Holística):** For a portfolio/demo project, scattered repositories fragment the narrative. A monorepo presents the full "Bank" as a cohesive product to reviewers and recruiters. / Para un proyecto de portafolio, los repositorios dispersos fragmentan la narrativa. Un monorepo presenta el "Banco" completo como un producto cohesivo.
* **Shared Knowledge (Conocimiento Compartido):** Documentation (`docs/`) lives right next to the code it describes, reducing the chance of stale diagrams. / La documentación vive junto al código, reduciendo diagramas desactualizados.

### 2. Why not Multi-Repo? / ¿Por qué no Multi-Repo?
* **Overhead:** Managing 5+ repositories (Auth, Core, Audit, Infra, Docs) requires setting up 5+ sets of secrets, permissions, and context switching. / Gestionar 5+ repositorios requiere configurar múltiples secretos, permisos y cambios de contexto.
* **Dependency Hell:** Sharing common DTOs or Utils between services in separate repos requires a private Artifactory/Nexus. In a monorepo, we can build them as local Maven modules. / Compartir DTOs entre servicios en repos separados requiere un Artifactory privado. En un monorepo, podemos construirlos como módulos Maven locales.

### 3. CI/CD Strategy / Estrategia CI/CD
The main risk of a monorepo is long build times (building everything on every commit). We mitigate this using **Path Filters**:
El principal riesgo es el tiempo de build largo. Mitigamos esto usando **Filtros de Rutas**:

* **Workflow: `infra-deploy.yml`**: Triggers ONLY if files in `terraform/**` change.
* **Workflow: `auth-service.yml`**: Triggers ONLY if files in `backend/auth-service/**` change.
* **Workflow: `docs-site.yml`**: Triggers ONLY if files in `docs/**` change.

```yaml
# Example / Ejemplo
on:
  push:
    paths:
      - 'backend/auth-service/**'
      - 'backend/pom.xml'
```

## Consequences / Consecuencias
* **Positive**: Simplified developer onboarding (one git clone). Unified versioning. Simplified "Atomic" refactors.

* **Positivo**: Onboarding de desarrolladores simplificado. Versionado unificado. Refactorizaciones "atómicas" simplificadas.

* **Negative**: The repository size grows larger over time. Strict discipline is required to enforce decoupling between modules (avoiding "Spaghetti Monolith").

* **Negativo**: El tamaño del repositorio crece con el tiempo. Se requiere disciplina estricta para forzar el desacoplamiento entre módulos (evitar "Monolito Espagueti").