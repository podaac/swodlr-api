/* -- Ingest -- */

// - KMS keys
resource "aws_kms_key" "ingest_sqs" {
  description = "${local.resource_prefix}-ingest-sqs"
  deletion_window_in_days = 10
}

resource "aws_kms_key_policy" "ingest_sqs" {
  key_id = aws_kms_key.ingest_sqs.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action = "kms:*"
        Resource = aws_kms_key.ingest_sqs.arn
      },

      {
        Sid = "Allow SNS topic to access the encryption keys"
        Effect = "Allow"
        Principal = {
          Service = "sns.amazonaws.com"
        }
        Action = [
          "kms:GenerateDataKey",
          "kms:Decrypt"
        ]
        Resource = aws_kms_key.ingest_sqs.arn
        Condition = {
          ArnEquals = {
            "aws:SourceArn" = aws_sns_topic.ingest.arn
          }
        }
      }
    ]
  })
}

resource "aws_kms_key" "ingest_sns" {
  description = "${local.resource_prefix}-ingest-sns"
  deletion_window_in_days = 10
}

resource "aws_kms_key_policy" "ingest_sns" {
  key_id = aws_kms_key.ingest_sns.id
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Sid = "Enable IAM User Permissions"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${data.aws_caller_identity.current.account_id}:root"
        }
        Action = "kms:*"
        Resource = aws_kms_key.ingest_sns.arn
      },

      {
        Sid = "Allow the ingest account access to the encryption keys"
        Effect = "Allow"
        Principal = {
          AWS = "arn:aws:iam::${var.ingest_aws_account}:role/${var.ingest_aws_role}"
        }
        Action = [
          "kms:GenerateDataKey*",
          "kms:Decrypt"
        ]
        Resource = aws_kms_key.ingest_sns.arn
      }
    ]
  })
}

resource "aws_kms_key" "product_create" {
  description = "${local.resource_prefix}-product-create"
  deletion_window_in_days = 10
}

resource "aws_kms_key" "product_update" {
  description = "${local.resource_prefix}-product-update"
  deletion_window_in_days = 10
}

resource "aws_kms_key" "async_update" {
  description = "${local.resource_prefix}-async-update"
  deletion_window_in_days = 10
}

// - SNS
resource "aws_sns_topic" "ingest" {
  name = "${local.resource_prefix}-ingest-topic"
  #kms_master_key_id = aws_kms_key.ingest_sns.id - CUMULUS-3292
}

resource "aws_sns_topic_subscription" "ingest_sqs_target" {
  topic_arn = aws_sns_topic.ingest.arn
  protocol = "sqs"
  endpoint = "arn:aws:sqs:${var.region}:${data.aws_caller_identity.current.account_id}:${aws_sqs_queue.ingest.name}"
  raw_message_delivery = true
}

resource "aws_sns_topic_policy" "ingest_topic_write" {
  arn = aws_sns_topic.ingest.arn

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        AWS = "arn:aws:iam::${var.ingest_aws_account}:root"
      },
      Action = "sns:Publish",
      Resource = aws_sns_topic.ingest.arn
    }]
  })
}

resource "aws_sns_topic" "product_update" {
  name = "${local.resource_prefix}-product-update-topic"
  kms_master_key_id = aws_kms_key.product_update.id
}

resource "aws_sns_topic_subscription" "async_update_target" {
  topic_arn = aws_sns_topic.product_update.arn
  protocol = "sqs"
  endpoint = aws_sqs_queue.async_update.arn
  raw_message_delivery = true
}

// - SQS
resource "aws_sqs_queue" "ingest" {
  name = "${local.resource_prefix}-ingest-queue"
  kms_master_key_id = aws_kms_key.ingest_sqs.id
  message_retention_seconds = 7 * 24 * 60 * 60  # 1 week
  visibility_timeout_seconds = 12 * 60 * 60     # 12 hours
}

resource "aws_sqs_queue_policy" "ingest" {
  queue_url = aws_sqs_queue.ingest.url
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "sns.amazonaws.com"
      }
      Action = "sqs:SendMessage"
      Resource = aws_sqs_queue.ingest.arn
      Condition = {
        ArnEquals = {
          "aws:SourceArn" = aws_sns_topic.ingest.arn
        }
      }
    }]
  })
}

resource "aws_sqs_queue" "product_create" {
  name = "${local.resource_prefix}-product-create-queue"
  kms_master_key_id = aws_kms_key.product_create.id
  message_retention_seconds = 7 * 24 * 60 * 60  # 1 week
  visibility_timeout_seconds = 12 * 60 * 60     # 12 hours
}

resource "aws_sqs_queue_policy" "product_create" {
  queue_url = aws_sqs_queue.product_create.url
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
      Action = "sqs:SendMessage"
      Resource = aws_sqs_queue.async_update.arn
      Condition = {
        ArnEquals = {
          "aws:SourceArn" = aws_ecs_task_definition.app.arn
        }
      }
    }]
  })
}

resource "aws_sqs_queue" "async_update" {
  name = "${local.resource_prefix}-async-update-queue"
  kms_master_key_id = aws_kms_key.async_update.id
  message_retention_seconds = 7 * 24 * 60 * 60  # 1 week
  visibility_timeout_seconds = 12 * 60 * 60     # 12 hours
}

resource "aws_sqs_queue_policy" "async_update" {
  queue_url = aws_sqs_queue.async_update.url
  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "sns.amazonaws.com"
      }
      Action = "sqs:SendMessage"
      Resource = aws_sqs_queue.async_update.arn
      Condition = {
        ArnEquals = {
          "aws:SourceArn" = aws_sns_topic.product_update.arn
        }
      }
    }]
  })
}
