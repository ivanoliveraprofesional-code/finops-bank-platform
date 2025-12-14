resource "aws_dynamodb_table" "audit_logs" {
  name           = "finops-audit-logs"
  billing_mode   = "PAY_PER_REQUEST" # FinOps: Solo pagas por lo que usas (Serverless)
  hash_key       = "TransactionId"
  range_key      = "Timestamp"

  attribute {
    name = "TransactionId"
    type = "S" # String
  }

  attribute {
    name = "Timestamp"
    type = "S"
  }

  # Encriptación Server-Side con nuestra llave KMS
  server_side_encryption {
    enabled     = true
    kms_key_arn = aws_kms_key.finops_key.arn
  }

  # Point-in-Time Recovery (Backup continuo para bancos)
  point_in_time_recovery {
    enabled = true
  }

  tags = {
    Domain = "Audit"
    DataClassification = "Sensitive"
  }
}