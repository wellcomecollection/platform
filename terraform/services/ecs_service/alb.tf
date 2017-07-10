resource "aws_alb_target_group" "ecs_service" {
  # We use snake case in a lot of places, but ALB Target Group names can
  # only contain alphanumerics and hyphens.
  name = "${replace(var.service_name, "_", "-")}"

  port     = 80
  protocol = "HTTP"
  vpc_id   = "${var.vpc_id}"

  health_check {
    protocol = "HTTP"
    path     = "${var.healthcheck_path}"
    matcher  = "200"
  }
}

module "listener_rule_https" {
  source           = "./listener_rule"
  host_name        = "${var.host_name}"
  listener_arn     = "${var.listener_https_arn}"
  alb_priority     = "${var.alb_priority}"
  target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
  path_pattern     = "${var.path_pattern}"
}

module "listener_rule_http" {
  source           = "./listener_rule"
  host_name        = "${var.host_name}"
  listener_arn     = "${var.listener_http_arn}"
  alb_priority     = "${var.alb_priority}"
  target_group_arn = "${aws_alb_target_group.ecs_service.arn}"
  path_pattern     = "${var.path_pattern}"
}
