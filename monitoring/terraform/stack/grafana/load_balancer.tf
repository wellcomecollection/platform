resource "aws_alb" "alb" {
  # This name can only contain alphanumerics and hyphens
  name = replace(var.namespace, "_", "-")

  subnets = var.public_subnets

  security_groups = [
    aws_security_group.service_lb_security_group.id,
    aws_security_group.external_lb_security_group.id,
  ]
}

resource "aws_alb_target_group" "ecs_service" {
  # We use snake case in a lot of places, but ALB Target Group names can
  # only contain alphanumerics and hyphens.
  name = replace(var.namespace, "_", "-")

  target_type = "ip"

  protocol = "HTTP"
  port     = local.container_port
  vpc_id   = var.vpc_id

  health_check {
    protocol = "HTTP"
    path     = "/api/health"
    matcher  = "200"
  }
}

resource "aws_alb_listener" "https" {
  load_balancer_arn = aws_alb.alb.id
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = data.aws_acm_certificate.certificate.arn

  default_action {
    target_group_arn = aws_alb_target_group.ecs_service.arn
    type             = "forward"
  }
}

data "aws_acm_certificate" "certificate" {
  domain   = var.domain
  statuses = ["ISSUED"]
}
