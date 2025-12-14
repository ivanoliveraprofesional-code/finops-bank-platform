resource "aws_sqs_queue" "audit_queue" {
  name                      = "finops-audit-queue"
  delay_seconds             = 0
  max_message_size          = 2048
  message_retention_seconds = 86400
  receive_wait_time_seconds = 10

  kms_master_key_id                 = aws_kms_key.finops_key.id
  kms_data_key_reuse_period_seconds = 300

  tags = {
    Environment = "Local"
    Service     = "Audit"
  }
}