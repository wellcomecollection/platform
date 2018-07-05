resource "aws_alb" "api_delta" {
  # This name can only contain alphanumerics and hyphens
  name = "${replace("${var.name}", "_", "-")}"

  subnets         = ["${var.public_subnets}"]
  security_groups = ["${concat(var.service_lb_security_group_ids, aws_security_group.external_lb_security_group.id)}"]
}

resource "aws_alb_listener" "https" {
  load_balancer_arn = "${aws_alb.api_delta.arn}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.certificate.arn}"

  default_action {
    target_group_arn = "${var.default_target_group_arn}"
    type             = "forward"
  }
}

data "aws_acm_certificate" "certificate" {
  domain   = "${var.certificate_domain}"
  statuses = ["ISSUED"]
}
