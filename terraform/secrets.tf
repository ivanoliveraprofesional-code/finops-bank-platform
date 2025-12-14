# --- 1. MASTER/MIGRATION USER (For Liquibase) ---
resource "random_password" "db_password" {
  length           = 16
  special          = false
}
# ----------------------

# Permissions: DDL (CREATE, DROP, ALTER) + DML
resource "aws_secretsmanager_secret" "db_owner_creds" {
  name        = "finops-bank/db-owner"
  description = "DDL Privileges (Liquibase/Migrations)"
}

resource "aws_secretsmanager_secret_version" "db_owner_val" {
  secret_id     = aws_secretsmanager_secret.db_owner_creds.id
  secret_string = jsonencode({
    username = "dbadmin"
    password = random_password.db_password.result
    # Hybrid Logic: Real RDS address in Prod, K8s DNS in Local
    host     = var.enable_rds ? aws_db_instance.main[0].address : "postgres-db.default.svc.cluster.local"
    port     = 5432
    dbname   = "finops_core_banking"
  })
}

# --- 2. APPLICATION USER (For Java App) ---
# Permissions: DML (SELECT, INSERT, UPDATE, DELETE)
resource "random_password" "app_password" {
  length  = 16
  special = false
}

resource "aws_secretsmanager_secret" "db_app_creds" {
  name        = "finops-bank/db-app"
  description = "DML Privileges (Java Microservice)"
}

resource "aws_secretsmanager_secret_version" "db_app_val" {
  secret_id     = aws_secretsmanager_secret.db_app_creds.id
  secret_string = jsonencode({
    username = "finops_app_user"
    password = random_password.app_password.result
    host     = var.enable_rds ? aws_db_instance.main[0].address : "postgres-db.default.svc.cluster.local"
    port     = 5432
    dbname   = "finops_core_banking"
  })
}

# --- 3. VIEWER USER (For Developers/Bastion) ---
# Permissions: READ ONLY (SELECT)
resource "random_password" "viewer_password" {
  length  = 16
  special = false
}

resource "aws_secretsmanager_secret" "db_viewer_creds" {
  name        = "finops-bank/db-viewer"
  description = "Read-Only Privileges (Developer Debugging)"
}

resource "aws_secretsmanager_secret_version" "db_viewer_val" {
  secret_id     = aws_secretsmanager_secret.db_viewer_creds.id
  secret_string = jsonencode({
    username = "finops_viewer"
    password = random_password.viewer_password.result
    host     = var.enable_rds ? aws_db_instance.main[0].address : "postgres-db.default.svc.cluster.local"
    port     = 5432
    dbname   = "finops_core_banking"
  })
}

# ==========================================
# AUTH SERVICE DATABASES (Identity Domain)
# ==========================================

# --- 1. AUTH OWNER (Liquibase) ---
resource "aws_secretsmanager_secret" "auth_db_owner" {
  name       = "finops-bank/auth-service/owner"
  kms_key_id = aws_kms_key.finops_key.id # ¡Encriptado con nuestra llave!
}

resource "aws_secretsmanager_secret_version" "auth_db_owner_val" {
  secret_id     = aws_secretsmanager_secret.auth_db_owner.id
  secret_string = jsonencode({
    username = "dbadmin"
    password = random_password.db_password.result
    host     = var.enable_rds ? aws_db_instance.main[0].address : "postgres-db.default.svc.cluster.local"
    port     = 5432
    dbname   = "finops_auth" # <--- DB Diferente
  })
}

# --- 2. AUTH APP USER ---
resource "random_password" "auth_app_password" {
  length = 16
  special = false
}

resource "aws_secretsmanager_secret" "auth_db_app" {
  name       = "finops-bank/auth-service/app"
  kms_key_id = aws_kms_key.finops_key.id
}

resource "aws_secretsmanager_secret_version" "auth_db_app_val" {
  secret_id     = aws_secretsmanager_secret.auth_db_app.id
  secret_string = jsonencode({
    username = "finops_auth_user"
    password = random_password.auth_app_password.result
    host     = var.enable_rds ? aws_db_instance.main[0].address : "postgres-db.default.svc.cluster.local"
    port     = 5432
    dbname   = "finops_auth"
  })
}

# --- 3. AUTH VIEWER ---
resource "random_password" "auth_viewer_password" {
  length = 16
  special = false
}

resource "aws_secretsmanager_secret" "auth_db_viewer" {
  name       = "finops-bank/auth-service/viewer"
  kms_key_id = aws_kms_key.finops_key.id
}

resource "aws_secretsmanager_secret_version" "auth_db_viewer_val" {
  secret_id     = aws_secretsmanager_secret.auth_db_viewer.id
  secret_string = jsonencode({
    username = "finops_auth_viewer"
    password = random_password.auth_viewer_password.result
    host     = var.enable_rds ? aws_db_instance.main[0].address : "postgres-db.default.svc.cluster.local"
    port     = 5432
    dbname   = "finops_auth"
  })
}