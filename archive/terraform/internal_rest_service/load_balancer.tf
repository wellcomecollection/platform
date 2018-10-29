resource "aws_lb" "network_load_balancer" {
  name               = "${replace("${var.service_name}", "_", "-")}-nlb"
  internal           = true
  load_balancer_type = "network"
  subnets            = ["${var.private_subnets}"]
}

data "aws_lb_target_group" "tcp_target_group" {
  name = "${module.service.target_group_name}"
}

resource "aws_lb_listener" "listener" {
  load_balancer_arn = "${aws_lb.network_load_balancer.arn}"
  port              = "80"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = "${data.aws_lb_target_group.tcp_target_group.arn}"
  }
}
