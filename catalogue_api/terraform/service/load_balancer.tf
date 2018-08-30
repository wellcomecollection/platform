resource "aws_alb_listener_rule" "https" {
  listener_arn = "${var.alb_listener_arn_https}"

  action {
    type             = "forward"
    target_group_arn = "${module.service.target_group_arn}"
  }

  condition {
    field  = "host-header"
    values = ["${var.host_name}"]
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }
}

resource "aws_alb_listener_rule" "http" {
  listener_arn = "${var.alb_listener_arn_http}"

  action {
    type             = "forward"
    target_group_arn = "${module.service.target_group_arn}"
  }

  condition {
    field  = "host-header"
    values = ["${var.host_name}"]
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }
}
