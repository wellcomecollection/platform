data "aws_lb_target_group" "target" {
  name = "${var.target_group_name}"
}

resource "aws_lb_listener" "listener" {
  load_balancer_arn = "${var.nlb_arn}"
  port              = "${var.listener_port}"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = "${data.aws_lb_target_group.target.arn}"
  }
}