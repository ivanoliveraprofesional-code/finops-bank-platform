#!/bin/bash
set -euo pipefail

echo "=== LocalStack init: creating DynamoDB table and SQS queue for audit-service ==="

echo "Creating DynamoDB table finops-audit-logs..."
awslocal dynamodb create-table \
  --table-name finops-audit-logs \
  --attribute-definitions \
      AttributeName=transactionId,AttributeType=S \
      AttributeName=timestamp,AttributeType=S \
  --key-schema \
      AttributeName=transactionId,KeyType=HASH \
      AttributeName=timestamp,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1 || echo "Table finops-audit-logs already exists (or creation failed)."

echo "Creating SQS queue finops-audit-queue..."
awslocal sqs create-queue \
  --queue-name finops-audit-queue \
  --region us-east-1 || echo "Queue finops-audit-queue already exists (or creation failed)."

echo "=== LocalStack Initialization Complete ==="
