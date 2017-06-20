resource "aws_alb_target_group" "ecs_service" {
  # We use snake case in a lot of places, but ALB Target Group names can
  # only contain alphanumerics and hyphens.
  name = "${replace(var.service_name, "_", "-")}"

  port     = 80
  protocol = "HTTP"
  vpc_id   = "${var.vpc_id}"

  health_check {
    path = "${var.healthcheck_path}"
  }
}

resource "aws_alb_listener_rule" "rule" {
  listener_arn = "${var.listener_arn}"
  priority     = "${var.alb_priority}"

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }
}
