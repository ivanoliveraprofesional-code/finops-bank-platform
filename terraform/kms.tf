resource "aws_kms_key" "finops_key" {
  description             = "Llave Maestra para encriptar datos del FinOps Bank"
  deletion_window_in_days = 10
  enable_key_rotation     = true # Rotación anual automática (Compliance)

  tags = {
    Name = "finops-bank-master-key"
  }
}

resource "aws_kms_alias" "finops_key_alias" {
  name          = "alias/finops-bank-key"
  target_key_id = aws_kms_key.finops_key.key_id
}