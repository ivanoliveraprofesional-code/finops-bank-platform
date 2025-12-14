variable "region" {
  description = "AWS Region"
  default     = "us-east-1"
}

variable "project_name" {
  description = "Project Name for tagging"
  default     = "finops-bank"
}

variable "vpc_cidr" {
  description = "CIDR block for the main VPC"
  default     = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  description = "CIDR for Public Subnet"
  default     = "10.0.1.0/24"
}

variable "private_app_cidr" {
  description = "CIDR for App Subnet"
  default     = "10.0.2.0/24"
}

variable "private_data_cidr" {
  description = "CIDR for Data Subnet"
  default     = "10.0.3.0/24"
}

variable "enable_eks" {
  description = "Controls if EKS resources should be created (False for LocalStack Free, True for AWS/Pro)"
  type        = bool
  default     = false
}

variable "enable_rds" {
  description = "Enable RDS creation (True for AWS, False for LocalStack Free)"
  type        = bool
  default     = false
}