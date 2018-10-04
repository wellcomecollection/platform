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
    type = "redirect"

    redirect {
      host        = "developers.wellcomecollection.org"
      path        = "/iiif"
      status_code = "HTTP_301"
    }
  }
}

# If Googlebot tries to crawl /robots.txt and gets a 500 error (for example,
# a gateway error if we haven't defined this path), it writes off the
# entire domain -- it becomes unable to index or cache images from
# iiif.wellcomecollection.org.
#
# This fixed response ensures that Googlebot gets a sensible response
# if it tries to crawl robots.txt, and that it can crawl the images.
#
resource "aws_alb_listener_rule" "robots_is_404" {
  listener_arn = "${aws_alb_listener.https.arn}"

  action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      status_code  = "404"
    }
  }

  condition {
    field  = "path-pattern"
    values = ["/robots.txt"]
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
    values = ["/image/*"]
  }
}

data "aws_acm_certificate" "certificate" {
  domain   = "${var.certificate_domain}"
  statuses = ["ISSUED"]
}
