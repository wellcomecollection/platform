resource "aws_alb" "api_delta" {
  # This name can only contain alphanumerics and hyphens
  name = "${replace("${var.name}", "_", "-")}"

  subnets         = ["${var.public_subnets}"]
  security_groups = ["${list(aws_security_group.service_lb_security_group.id, aws_security_group.external_lb_security_group.id)}"]
}

resource "aws_alb_listener" "https" {
  load_balancer_arn = "${aws_alb.api_delta.arn}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.certificate.arn}"

  default_action {
    type = "redirect"

    redirect {
      host        = "${var.top_level_host}"
      path        = "${var.top_level_path}"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = "${aws_alb.api_delta.arn}"
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

data "aws_acm_certificate" "certificate" {
  domain   = "${var.certificate_domain}"
  statuses = ["ISSUED"]
}
