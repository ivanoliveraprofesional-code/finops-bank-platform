# FinOps Distributed Banking Platform
### Plataforma Bancaria Distribuida FinOps

**A Cloud-Native, High-Security Banking Core Simulation.**
**Una simulación de Núcleo Bancario Nativo en la Nube y de Alta Seguridad.**

---

## 1. Executive Summary / Resumen Ejecutivo

This project demonstrates a production-grade **Tier-3 Architecture** running locally. It bridges the gap between Software Development and Platform Engineering, focusing on **Cost Optimization (FinOps)**, **Zero Trust Security**, and **GitOps**.

Este proyecto demuestra una **Arquitectura Tier-3** de grado de producción ejecutándose localmente. Cierra la brecha entre el Desarrollo de Software y la Ingeniería de Plataforma, enfocándose en **Optimización de Costos (FinOps)**, **Seguridad Zero Trust** y **GitOps**.

**Key Achievements / Logros Clave:**
* **$0 Cloud Bill:** Uses LocalStack to emulate AWS services (S3, KMS, Secrets Manager, DynamoDB). / **Factura de Nube $0:** Usa LocalStack para emular servicios AWS.
* **Hybrid Database Strategy:** Architecture agnostic to RDS (Prod) vs Containers (Dev). / **Estrategia Híbrida:** Arquitectura agnóstica entre RDS (Prod) y Contenedores (Dev).
* **Strict Security:** Implements **Istio Service Mesh** for Ingress, mTLS, and JWT Validation. / **Seguridad Estricta:** Implementa Istio Service Mesh para Ingress, mTLS y validación JWT.

### Architecture Overview / Visión General de Arquitectura
![Container View](images/container-view.png)

---

## 2. Project Structure / Estructura del Proyecto (Monorepo)

This repository follows a **Structured Monorepo** pattern to unify Infrastructure, Backend, and Documentation.
Este repositorio sigue un patrón de **Monorepo Estructurado** para unificar Infraestructura, Backend y Documentación.

```text
finops-bank-platform/
├── backend/                # Java 21 Microservices (Hexagonal Arch)
│   ├── auth-service/       # Identity Provider (OIDC)
│   ├── core-banking/       # Transaction Ledger
│   ├── audit-service/      # SQS Consumer (Worker)
│   └── credit-risk/        # gRPC Risk Engine
├── terraform/              # Infrastructure as Code (AWS/LocalStack)
│   └── main.tf             # Resource Definitions
├── kubernetes/             # K8s Manifests (ArgoCD Ready)
│   ├── platform/           # DB, Istio, ConfigMaps
│   ├── security/           # Istio Policies (JWT, mTLS)
│   └── apps/               # Application Deployments
├── scripts/                # Automation Scripts (PowerShell)
├── docs/                   # Architecture Diagrams (C4) & ADRs
├── .github/workflows/      # CI/CD Pipelines (Path-Based)
└── docker-compose.yml      # Local Cloud Emulator
```

---
## 3. Tech Stack / Stack Tecnológico

| Area | Technology | Purpose / Propósito |
|------|------------|---------------------|
| **Infrastructure** | Terraform (HCL) | Infrastructure as Code (IaC) with State Locking |
| **Cloud Emulation** | LocalStack | AWS API Mocking (S3, DynamoDB, Secrets, KMS) |
| **Orchestration** | Kubernetes (Kind) | Container Orchestration |
| **Ingress & Security** | Istio Service Mesh | Gateway, mTLS, JWT Validation (Zero Trust) |
| **Backend** | Java 21 + Spring Boot 3.2 | Hexagonal Architecture Microservices |
| **Database** | PostgreSQL 13 | Relational Data (Auth & Transactions) |
| **NoSQL** | DynamoDB | Audit Logs & High-Volume Data |
| **Migration** |	Liquibase |	Database Schema Management |
| **Communication** |	gRPC (Protobuf) |	Low-latency internal service communication |

---

## 4. Quick Start / Inicio Rápido

**Prerequisites / Prerrequisitos:**
* Docker Desktop (Allocated RAM: 6GB+)
* Terraform >= 1.5
* AWS CLI
* Python 3.9+ (For Lambda dependencies)
* kubectl & kind

**One-Click Deployment Infraestructure / Despliegue infraestructura en un Click:**
We use a unified automation script to bootstrap infrastructure, deploy K8s, apply security layers, and run smoke tests. 
Usamos un script unificado para iniciar la infraestructura, K8s, capas de seguridad y pruebas de humo.

```powershell
# Run from root / Ejecutar desde la raíz
.\start-all.ps1
```

To destroy everything / Para destruir todo:

```powershell
# Run from root / Ejecutar desde la raíz
.\stop-all.ps1
```

**Start Infrastructure / Iniciar Infraestructura**
Starts Postgres & LocalStack (SQS/DynamoDB)

```bash
docker-compose -f backend/docker-compose-dev.yml up -d 
```

**Run Services / Correr Servicios**

```bash
cd backend/core-banking && mvn spring-boot:run
```

```bash
cd backend/core-banking && mvn spring-boot:run
```

```bash
cd backend/audit-service && mvn spring-boot:run
```

```bash
cd backend/credit-risk-service && mvn spring-boot:run
```

---

## 5. Operational Runbooks / Manuales Operativos

**Accessing the Database (Zero Trust) / Acceso a Base de Datos**
* Constraint: Direct access to the DB (localhost:5432) is blocked by Network Policies. You must use the ephemeral Bastion. Restricción: El acceso directo a la BD está bloqueado por Políticas de Red. Debe usar el Bastion efímero.

```bash
kubectl run bastion-sql --rm -it --labels="app=bastion-sql" --image=postgres:alpine --restart=Never -- \
  psql postgresql://finops_viewer:PASSWORD@postgres-db.default.svc.cluster.local/finops_core_banking
```

(Get the PASSWORD from AWS Secrets Manager / Obtenga el PASSWORD de AWS Secrets Manager)

---

## 6. Documentation / Documentación
[**Architecture Diagrams (C4 & Network) / Diagramas de Arquitectura**](architecture.md) - Visual deep dive.

[**Architecture Decision Records (ADRs)**](adr/) - Technical decisions log (Why Monorepo? Why Istio?).


