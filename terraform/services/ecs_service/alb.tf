resource "aws_alb_listener" "ecs_service" {
  load_balancer_arn = "${var.alb_id}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${var.acm_cert_arn}"

  default_action {
    target_group_arn = "${aws_alb_target_group.ecs_service.id}"
    type             = "forward"
  }
}

resource "aws_alb_target_group" "ecs_service" {
  name     = "${var.service_name}"
  port     = 80
  protocol = "HTTP"
  vpc_id   = "${var.vpc_id}"

  health_check {
    path = "/management/healthcheck"
  }
}
