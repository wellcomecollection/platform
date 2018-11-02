resource "aws_lb" "network_load_balancer" {
  name               = "${replace("${local.namespace}", "_", "-")}-nlb"
  internal           = true
  load_balancer_type = "network"
  subnets            = ["${local.private_subnets}"]
}

resource "aws_lb_listener" "progress_listener" {
  load_balancer_arn = "${aws_lb.network_load_balancer.arn}"
  port              = "${local.progress_http_lb_port}"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = "${module.progress_http.target_group_arn}"
  }
}

resource "aws_lb_listener" "registrar_listener" {
  load_balancer_arn = "${aws_lb.network_load_balancer.arn}"
  port              = "${local.registrar_http_lb_port}"
  protocol          = "TCP"

  default_action {
    type             = "forward"
    target_group_arn = "${module.registrar_http.target_group_arn}"
  }
}
