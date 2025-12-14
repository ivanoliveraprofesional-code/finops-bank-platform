# 1. Security Group para los Nodos de EKS (App)
resource "aws_security_group" "app_nodes_sg" {
  name        = "${var.project_name}-app-nodes-sg"
  description = "Security Group for K8s Nodes"
  vpc_id      = aws_vpc.main.id
  
  # Outbound: Permitir todo (para bajar imágenes docker, etc)
  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# 2. Security Group para la Base de Datos (RDS)
resource "aws_security_group" "rds_sg" {
  count       = var.enable_rds ? 1 : 0
  name        = "${var.project_name}-rds-sg"
  description = "Allow inbound traffic ONLY from App Nodes"
  vpc_id      = aws_vpc.main.id

  # INGRESS ESTRICTO: Solo permite tráfico del SG de la App
  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app_nodes_sg.id] # <--- REFERENCIA DIRECTA
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

# --- SUBNET PLACEMENT ---
resource "aws_db_subnet_group" "main" {
  count      = var.enable_rds ? 1 : 0
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = [aws_subnet.private_data.id, aws_subnet.private_app.id]

  tags = { Name = "My DB subnet group" }
}

# --- DATABASE INSTANCE ---
resource "aws_db_instance" "main" {
  count                  = var.enable_rds ? 1 : 0
  identifier             = "${var.project_name}-postgres"
  allocated_storage      = 20
  db_name                = "finops_core_banking"
  engine                 = "postgres"
  engine_version         = "13"
  instance_class         = "db.t3.micro"
  username               = "dbadmin"
  
  # It is perfectly legal to reference a resource defined in secrets.tf
  password               = random_password.db_password.result
  
  # Logic for Feature Flags
  db_subnet_group_name   = aws_db_subnet_group.main[0].name 
  vpc_security_group_ids = [aws_security_group.rds_sg[0].id]

  skip_final_snapshot    = true
  publicly_accessible    = false 
}