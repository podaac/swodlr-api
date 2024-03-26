/* -- Load balancer -- */
resource "aws_lb" "app" {
  name = "${local.resource_prefix}-alb"
  load_balancer_type = "application"
  enable_cross_zone_load_balancing = true
  internal = true
  subnets = data.aws_subnets.private.ids
  security_groups = [aws_security_group.load_balancer.id]
}

resource "aws_lb_listener" "app" {
  load_balancer_arn = aws_lb.app.arn
  port = 443
  protocol = "HTTPS"
  certificate_arn = aws_acm_certificate.cert.arn
  ssl_policy = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  default_action {
    type = "forward"
    target_group_arn = aws_lb_target_group.app.arn
  }
}

resource "aws_lb_target_group" "app" {
  name = "${local.resource_prefix}-app-lb-tg"
  port = 443
  protocol = "HTTPS"
  target_type = "ip"
  vpc_id = data.aws_vpc.default.id

  health_check {
    enabled = true
    matcher = "200"
    path = "${var.app_base_path}/about"
    port = 443
  }
}

/* -- Security Group -- */
resource "aws_security_group" "load_balancer" {
  vpc_id = data.aws_vpc.default.id
  name = "${local.resource_prefix}-lb-sg"

  ingress {
    from_port = 443
    to_port   = 443
    protocol  = "tcp"
    cidr_blocks    = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }

  egress {
    from_port    = 0
    to_port      = 0
    protocol     = "-1"
    cidr_blocks    = ["0.0.0.0/0"]
    ipv6_cidr_blocks = ["::/0"]
  }
}

/* --  Load balancer listener certificate -- */
resource "aws_acm_certificate" "cert" {
  domain_name       = "${local.name}.${local.environment}.internal.earthdatacloud.nasa.gov"
 
  certificate_authority_arn = data.aws_ssm_parameter.private_ca.value
 
  lifecycle {
    create_before_destroy = true
  }
}