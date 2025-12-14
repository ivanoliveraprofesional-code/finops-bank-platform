resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name    = "${var.project_name}-vpc"
    Project = "FinOps Distributed Banking Platform"
  }
}

# --- PUBLIC LAYER ---
resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.main.id
  tags = { Name = "${var.project_name}-igw" }
}

resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidr
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "${var.project_name}-public-subnet"
    Tier = "Public"
    "kubernetes.io/role/elb" = "1" # Tag necesario para que EKS sepa dónde poner Load Balancers
  }
}

# --- NAT GATEWAY (La pieza que faltaba) ---
resource "aws_eip" "nat" {
  domain = "vpc"
  tags   = { Name = "${var.project_name}-nat-eip" }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public.id # El NAT vive en la pública

  tags = { Name = "${var.project_name}-nat-gw" }

  depends_on = [aws_internet_gateway.igw]
}

# --- PRIVATE LAYER (APP) ---
resource "aws_subnet" "private_app" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_app_cidr
  availability_zone = "${var.region}a"

  tags = {
    Name = "${var.project_name}-private-app-subnet"
    Tier = "App"
    "kubernetes.io/role/internal-elb" = "1" # Para Load Balancers internos
  }
}

# --- PRIVATE LAYER (DATA) ---
resource "aws_subnet" "private_data" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_data_cidr
  availability_zone = "${var.region}a"

  tags = {
    Name = "${var.project_name}-private-data-subnet"
    Tier = "Data"
  }
}

# --- ROUTING ---
# Pública: Sale por IGW
resource "aws_route_table" "public_rt" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
  tags = { Name = "${var.project_name}-public-rt" }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public_rt.id
}

# Privada: Sale por NAT Gateway
resource "aws_route_table" "private_rt" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }
  tags = { Name = "${var.project_name}-private-rt" }
}

resource "aws_route_table_association" "app" {
  subnet_id      = aws_subnet.private_app.id
  route_table_id = aws_route_table.private_rt.id
}

resource "aws_route_table_association" "data" {
  subnet_id      = aws_subnet.private_data.id
  route_table_id = aws_route_table.private_rt.id
}