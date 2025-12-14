terraform {
  # 1. Lock Terraform Core Version
  required_version = ">= 1.5.0, < 2.0.0"

  required_providers {
    # 2. Lock AWS Provider
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.30" 
    }

    # 3. Lock Random Provider
    random = {
      source  = "hashicorp/random"
      version = "~> 3.6"
    }
  }

  # 4. State Locking & Backend
  backend "s3" {
    bucket                      = "terraform-state"
    key                         = "finops-platform/terraform.tfstate"
    region                      = "us-east-1"
    
    # --- FIX CRÍTICO: Definir endpoints para AMBOS servicios ---
    endpoint                    = "http://localhost:4566" 
    dynamodb_endpoint           = "http://localhost:4566" 
    # -----------------------------------------------------------

    # Security checks skip
    skip_credentials_validation = true
    skip_metadata_api_check     = true
    skip_region_validation      = true
    skip_requesting_account_id  = true
    
    # Path style obligatorio para LocalStack
    force_path_style            = true
    
    # State Locking via DynamoDB
    dynamodb_table              = "terraform-lock" 
  }
}