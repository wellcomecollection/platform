resource "aws_alb_target_group" "ecs_service" {
  name     = "${var.service_name}"
  port     = 80
  protocol = "HTTP"
  vpc_id   = "${var.vpc_id}"

  health_check {
    path = "/management/healthcheck"
  }
}

resource "aws_alb_listener_rule" "rule" {
  listener_arn = "${var.listener_arn}"
  priority     = 100

  action {
    type             = "forward"
    target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }
}
