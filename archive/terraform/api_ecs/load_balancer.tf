resource "aws_alb" "services" {
  # This name can only contain alphanumerics and hyphens
  name = "${replace("${var.namespace}_api", "_", "-")}"

  subnets         = ["${var.public_subnets}"]
  security_groups = ["${aws_security_group.external_lb_security_group.id}", "${aws_security_group.service_lb_security_group.id}"]
}

resource "aws_alb_listener" "api_https" {
  load_balancer_arn = "${aws_alb.services.id}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.certificate.arn}"

  default_action {
    target_group_arn = "${module.api_ecs.target_group_arn}"
    type             = "forward"
  }
}

resource "aws_lb_listener" "api_http" {
  load_balancer_arn = "${aws_alb.services.id}"
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = 443
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_alb_listener_rule" "path_rule" {
  listener_arn = "${aws_alb_listener.api_https.arn}"

  action {
    type             = "forward"
    target_group_arn = "${module.api_ecs.target_group_arn}"
  }

  condition {
    field  = "path-pattern"
    values = ["${var.api_path}"]
  }
}

data "aws_acm_certificate" "certificate" {
  domain   = "${var.certificate_domain}"
  statuses = ["ISSUED"]
}
