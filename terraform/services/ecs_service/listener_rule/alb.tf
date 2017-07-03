# When using the module, the user can specify a path pattern and (optionally)
# a hostname pattern.  We only want a path rule OR a hostname/path rule, but
# not both.  We use the `count` parameter to only create one of these.

resource "aws_alb_listener_rule" "path_rule" {
  count        = "${var.host_name == "" ? 1 : 0}"
  listener_arn = "${var.listener_arn}"
  priority     = "${var.alb_priority}"

  action {
    type             = "forward"
    target_group_arn = "${var.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }
}

resource "aws_alb_listener_rule" "path_host_rule" {
  count        = "${var.host_name != "" ? 1 : 0}"
  listener_arn = "${var.listener_arn}"
  priority     = "${var.alb_priority}"

  action {
    type             = "forward"
    target_group_arn = "${var.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }

  condition {
    field  = "host-header"
    values = ["${var.host_name}"]
  }
}
