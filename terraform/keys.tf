resource "tls_private_key" "jwt_rsa" {
  algorithm = "RSA"
  rsa_bits  = 2048
}

resource "aws_secretsmanager_secret" "jwt_private_key" {
  name        = "finops-bank/auth/jwt-private-key"
  description = "RSA Private Key for signing JWTs (PKCS#8)"
  kms_key_id  = aws_kms_key.finops_key.id
}

resource "aws_secretsmanager_secret_version" "jwt_private_key_val" {
  secret_id     = aws_secretsmanager_secret.jwt_private_key.id
  secret_string = tls_private_key.jwt_rsa.private_key_pem_pkcs8
}

resource "aws_secretsmanager_secret" "jwt_public_key" {
  name        = "finops-bank/auth/jwt-public-key"
  description = "RSA Public Key for verifying JWTs"
  kms_key_id  = aws_kms_key.finops_key.id
}

resource "aws_secretsmanager_secret_version" "jwt_public_key_val" {
  secret_id     = aws_secretsmanager_secret.jwt_public_key.id
  secret_string = tls_private_key.jwt_rsa.public_key_pem
}