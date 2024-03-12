/* -- ECS -- */
locals {
  container_image = "${var.container_image}:${var.container_image_tag}"
  container_vcpu = 2
  container_memory = 4096
}

resource "aws_ecs_cluster" "default" {
  name = "${local.resource_prefix}-cluster"

  configuration {
    execute_command_configuration {
      kms_key_id = aws_kms_key.app_log.id
      logging = "OVERRIDE"

      log_configuration {
        cloud_watch_encryption_enabled = true
        cloud_watch_log_group_name = aws_cloudwatch_log_group.app.name
      }
    }
  }
}

resource "aws_ecs_cluster_capacity_providers" "fargate" {
  cluster_name = aws_ecs_cluster.default.name
  capacity_providers = ["FARGATE"]
}

resource "aws_ecs_service" "app" {
  name = "${local.resource_prefix}-app"
  cluster = aws_ecs_cluster.default.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count = 1
  launch_type = "FARGATE"
  force_new_deployment = true

  load_balancer {
    target_group_arn = aws_lb_target_group.app.arn
    container_name = "${local.resource_prefix}-app"
    container_port = 8080
  }

  network_configuration {
    subnets = data.aws_subnets.private.ids
    security_groups = [
      aws_security_group.app.id,
      aws_security_group.database.id
    ]
  }
}

locals {
  swodlr_profiles_active = length(var.active_profiles) > 0 ? [{
    name = "SPRING_PROFILES_ACTIVE",
    value = join(",", var.active_profiles)
  }] : []

  swodlr_app_env = concat([], local.swodlr_profiles_active)
}

resource "aws_ecs_task_definition" "app" {
  family = "${local.resource_prefix}-app-task"
  requires_compatibilities = ["FARGATE"]

  network_mode = "awsvpc"
  cpu = 1024 * local.container_vcpu
  memory = local.container_memory

  container_definitions = jsonencode([{
    name = "${local.resource_prefix}-app"
    image = local.container_image

    environment = local.swodlr_app_env

    portMappings = [{
      containerPort = 8080
      hostPort = 8080
    }]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-region = var.region
        awslogs-group = aws_cloudwatch_log_group.app.name
        awslogs-stream-prefix = local.resource_prefix
      }
    }
  }])

  task_role_arn = aws_iam_role.app_task.arn
  execution_role_arn = aws_iam_role.app_task_exec.arn
}

/* -- Cloudwatch -- */
resource "aws_cloudwatch_log_group" "app" {
  name = local.app_path
  retention_in_days = 30
}

/* -- App Config -- */
resource "aws_ssm_parameter" "app_base_path" {
  name = "${local.app_path}/spring.webflux.base-path"
  type = "String"
  value = var.app_base_path
}

resource "aws_ssm_parameter" "app_db_url" {
  name = "${local.app_path}/spring.datasource.url"
  type = "String"
  value = "jdbc:postgresql://${aws_db_instance.database.endpoint}/${var.db_name}"
}

resource "aws_ssm_parameter" "app_db_username" {
  name = "${local.app_path}/spring.datasource.username"
  type = "String"
  value = aws_ssm_parameter.db_app_username.value
}

resource "aws_ssm_parameter" "app_db_password" {
  name = "${local.app_path}/spring.datasource.password"
  type = "SecureString"
  value = aws_ssm_parameter.db_app_password.value
}

resource "aws_ssm_parameter" "app_edl_base_path" {
  name = "${local.app_path}/swodlr.security.edl-base-url"
  type = "String"
  value = var.edl_base_url
}

resource "aws_ssm_parameter" "app_edl_client_id" {
  name = "${local.app_path}/swodlr.security.edl-client-id"
  type = "String"
  value = var.edl_client_id
}

resource "aws_ssm_parameter" "app_edl_client_secret" {
  name = "${local.app_path}/swodlr.security.edl-client-secret"
  type = "String"
  value = var.edl_client_secret
}

resource "aws_ssm_parameter" "app_session_encryption_key" {
  name = "${local.app_path}/swodlr.security.session-encryption-key"
  type = "SecureString"
  value = var.session_encryption_key
}

resource "aws_ssm_parameter" "app_frontend_uri_pattern" {
  name = "${local.app_path}/swodlr.security.frontend-uri-pattern"
  count = length(var.frontend_uri_pattern) > 0 ? 1 : 0
  type = "String"
  value = var.frontend_uri_pattern
}

resource "aws_ssm_parameter" "tea_mapping" {
  for_each = var.tea_mapping

  name = "${local.app_path}/swodlr.tea-mapping.${each.key}"
  type = "String"
  value = each.value
}

resource "aws_ssm_parameter" "app_product_create_queue_url" {
  name = "${local.app_path}/swodlr.product-create-queue-url"
  type = "SecureString"
  value = aws_sqs_queue.product_create.url
}

resource "aws_ssm_parameter" "available_tiles_table_name" {
  name = "${local.app_path}/swodlr.available-tiles-table-name"
  type = "String"
  value = aws_dynamodb_table.available_tiles.name
}

/* -- Security -- */
resource "aws_security_group" "app" {
  name = "${local.resource_prefix}-app-sg"
  vpc_id = data.aws_vpc.default.id

  ingress {
    from_port = 8080
    to_port   = 8080
    protocol  = "tcp"
    cidr_blocks = [data.aws_vpc.default.cidr_block]
    // IPv6 is not supported NGAP VPCs
    //ipv6_cidr_blocks = [data.aws_vpc.default.ipv6_cidr_block]
    self = true
  }

  egress {
    from_port        = 0
    to_port          = 0
    protocol         = "-1"
    cidr_blocks      = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

resource "aws_kms_key" "app_log" {
  description = "${local.resource_prefix}-app-log"
  deletion_window_in_days = 7
}

/* -- IAM -- */
resource "aws_iam_role" "app_task" {
  name = "ecs-task-role"
  path = "${local.app_path}/"

  permissions_boundary = "arn:aws:iam::${local.account_id}:policy/NGAPShRoleBoundary"
  managed_policy_arns = [
    "arn:aws:iam::${local.account_id}:policy/NGAPProtAppInstanceMinimalPolicy"
  ]

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Sid = ""
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })

  inline_policy {
    name = "AllowSSM"
    policy = jsonencode({
      Version = "2012-10-17"
      Statement = [
        {
          "Sid" = ""
          Action = [
            "ssm:GetParameter",
            "ssm:GetParameters",
            "ssm:GetParametersByPath"
          ]
          Effect = "Allow"
          Resource = "arn:aws:ssm:${var.region}:${local.account_id}:parameter${local.service_path}/*"
        }
      ]
    })
  }

  inline_policy {
    name = "AllowProductQueue"
    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Sid = ""
          Effect = "Allow",
          Action = [
            "sqs:SendMessage"
          ]
          Resource = aws_sqs_queue.product_create.arn
        }
      ]
    })
  }

  inline_policy {
    name = "AllowQueryAvailableTiles"
    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Sid = ""
          Effect = "Allow",
          Action = ["dynamodb:BatchGetItem", "dynamodb:GetItem"]
          Resource = aws_dynamodb_table.available_tiles.arn
        }
      ]
    })
  }
}

resource "aws_iam_role" "app_task_exec" {
  name = "ecs-task-exec-role"
  path = "${local.app_path}/"

  permissions_boundary = "arn:aws:iam::${local.account_id}:policy/NGAPShRoleBoundary"
  managed_policy_arns = [
    "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  ]

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Sid = ""
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })

  inline_policy {
    name = "allow-logging"
    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Sid = ""
          Effect = "Allow",
          Action = [
            "logs:CreateLogStream",
            "logs:PutLogEvents"
          ]
          Resource = "*"
        }
      ]
    })
  }

  inline_policy {
    name = "allow-avalible-tiles-table"
    policy = jsonencode({
      Version = "2012-10-17",
      Statement = [
        {
          Sid = ""
          Action = [
            "dynamodb:BatchGetItem",
            "dynamodb:GetItem"
          ]
          Effect   = "Allow"
          Resource = aws_dynamodb_table.available_tiles.arn
        }
      ]
    })
  }
}
