resource "aws_alb" "public_services" {
  # This name can only contain alphanumerics and hyphens
  name = "${replace("${local.namespace}", "_", "-")}"

  subnets         = ["${module.network.public_subnets}"]
  security_groups = ["${aws_security_group.service_lb_security_group.id}", "${aws_security_group.external_lb_security_group.id}"]
}

resource "aws_alb_listener" "http_80" {
  load_balancer_arn = "${aws_alb.public_services.id}"
  port              = "80"
  protocol          = "HTTP"

  default_action {
    target_group_arn = "${module.service.target_group_arn}"
    type             = "forward"
  }
}