resource "aws_lb" "network_load_balancer" {
  name               = "${var.service_name}-nlb"
  internal           = true
  load_balancer_type = "network"
  subnets            = ["${var.private_subnets}"]
}


resource "aws_lb_listener" "listener" {
  load_balancer_arn = "${aws_lb.network_load_balancer.arn}"
  port              = "80"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = "${module.service.target_group_arn[0]}"
  }
}