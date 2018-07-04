resource "aws_alb" "public_services" {
  # This name can only contain alphanumerics and hyphens
  name = "${replace("${var.namespace}", "_", "-")}"

  subnets         = ["${var.public_subnets}"]
  security_groups = ["${aws_security_group.service_lb_security_group.id}", "${aws_security_group.external_lb_security_group.id}"]
}

resource "aws_alb_listener" "https" {
  load_balancer_arn = "${aws_alb.public_services.id}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.certificate.arn}"

  default_action {
    target_group_arn = "${module.service.target_group_arn}"
    type             = "forward"
  }
}

resource "aws_alb_listener_rule" "https" {
  listener_arn = "${aws_alb_listener.https.arn}"

  action {
    type             = "forward"
    target_group_arn = "${module.service.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["${var.path_pattern}"]
  }
}

data "aws_acm_certificate" "certificate" {
  domain   = "${var.certificate_domain}"
  statuses = ["ISSUED"]
}
