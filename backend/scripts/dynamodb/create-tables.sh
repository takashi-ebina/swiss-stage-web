#!/bin/bash

# DynamoDB Localのテーブル作成スクリプト
# 参加者一覧管理機能用のGroupテーブルとParticipantテーブルを作成

set -e

ENDPOINT_URL="${DYNAMODB_ENDPOINT:-http://localhost:8000}"
REGION="${AWS_REGION:-ap-northeast-1}"

echo "Creating DynamoDB tables for participant list management..."
echo "Endpoint: $ENDPOINT_URL"
echo "Region: $REGION"

# Groupテーブル作成
echo ""
echo "Creating Group table..."
aws dynamodb create-table \
  --table-name Group \
  --attribute-definitions \
    AttributeName=tournamentId,AttributeType=S \
    AttributeName=groupNumber,AttributeType=N \
  --key-schema \
    AttributeName=tournamentId,KeyType=HASH \
    AttributeName=groupNumber,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url "$ENDPOINT_URL" \
  --region "$REGION" \
  2>/dev/null || echo "Group table already exists or error occurred"

# Participantテーブル作成
echo ""
echo "Creating Participant table..."
aws dynamodb create-table \
  --table-name Participant \
  --attribute-definitions \
    AttributeName=groupId,AttributeType=S \
    AttributeName=participantId,AttributeType=S \
    AttributeName=rankLevel,AttributeType=N \
    AttributeName=registrationOrder,AttributeType=N \
  --key-schema \
    AttributeName=groupId,KeyType=HASH \
    AttributeName=participantId,KeyType=RANGE \
  --global-secondary-indexes \
    "[
      {
        \"IndexName\": \"groupId-rankLevel-index\",
        \"KeySchema\": [
          {\"AttributeName\": \"groupId\", \"KeyType\": \"HASH\"},
          {\"AttributeName\": \"rankLevel\", \"KeyType\": \"RANGE\"}
        ],
        \"Projection\": {
          \"ProjectionType\": \"ALL\"
        }
      },
      {
        \"IndexName\": \"groupId-registrationOrder-index\",
        \"KeySchema\": [
          {\"AttributeName\": \"groupId\", \"KeyType\": \"HASH\"},
          {\"AttributeName\": \"registrationOrder\", \"KeyType\": \"RANGE\"}
        ],
        \"Projection\": {
          \"ProjectionType\": \"ALL\"
        }
      }
    ]" \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url "$ENDPOINT_URL" \
  --region "$REGION" \
  2>/dev/null || echo "Participant table already exists or error occurred"

echo ""
echo "Listing tables..."
aws dynamodb list-tables \
  --endpoint-url "$ENDPOINT_URL" \
  --region "$REGION"

echo ""
echo "Table creation completed!"
