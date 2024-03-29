/* -- Security Group -- */
resource "aws_security_group" "database" {
  vpc_id = data.aws_vpc.default.id
  name = "${local.resource_prefix}-db-sg"

  ingress {
    from_port = 5432
    to_port   = 5432
    protocol  = "tcp"
    self    = true
  }

  egress {
    from_port    = 0
    to_port      = 0
    protocol     = "-1"
    cidr_blocks    = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

/* -- Subnet Group -- */
resource "aws_db_subnet_group" "default" {
  name = "${local.resource_prefix}-subnet"
  subnet_ids = [for k, v in data.aws_subnet.private : v.id]
}

/* -- Authentication Credentials -- */
resource "random_pet" "admin_username" {
  separator = "_"
}

resource "random_pet" "app_username" {
  separator = "_"
}

resource "random_password" "admin_password" {
  length = 32
  override_special = "!#$%&*()-_=+[]{}<>?"
}

resource "random_password" "app_password" {
  length = 32
  override_special = "!#$%&*()-_=+[]{}<>?"
}

/* -- KMS Key -- */
resource "aws_kms_key" "database" {
  description = "${local.resource_prefix}-rds"
}

/* -- Database -- */
resource "aws_db_instance" "database" {
  identifier = "${local.resource_prefix}-rds"

  instance_class = "db.t3.micro"
  allocated_storage = 20
  storage_type = "gp2"
  skip_final_snapshot = true
  multi_az = true
  storage_encrypted = true
  kms_key_id = aws_kms_key.database.arn

  vpc_security_group_ids = [aws_security_group.database.id]
  db_subnet_group_name = aws_db_subnet_group.default.id

  engine = "postgres"
  engine_version = "14.10"

  username = random_pet.admin_username.id
  password = random_password.admin_password.result
}

/* -- SSM Parameter Store -- */
resource "aws_ssm_parameter" "db_admin_username" {
  name = "${local.service_path}/db-admin-username"
  type = "String"
  value = aws_db_instance.database.username
}

resource "aws_ssm_parameter" "db_admin_password" {
  name = "${local.service_path}/db-admin-password"
  type = "SecureString"
  value = aws_db_instance.database.password
}

resource "aws_ssm_parameter" "db_app_username" {
  name = "${local.service_path}/db-app-username"
  type = "String"
  value = random_pet.app_username.id
}

resource "aws_ssm_parameter" "db_app_password" {
  name = "${local.service_path}/db-app-password"
  type = "SecureString"
  value = random_password.app_password.result
}

resource "aws_ssm_parameter" "db_name" {
  name = "${local.service_path}/db-name"
  type = "String"
  value = var.db_name
}
