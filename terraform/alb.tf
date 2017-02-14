resource "aws_alb" "main" {
  name            = "platform-alb"
  subnets         = ["${aws_subnet.main.*.id}"]
  security_groups = ["${aws_security_group.lb_sg.id}"]
}

resource "aws_alb" "tools" {
  name            = "platform-tools-alb"
  subnets         = ["${aws_subnet.tools.*.id}"]
  security_groups = ["${aws_security_group.tools_lb_sg.id}"]
}

resource "aws_alb_listener" "platform_api_listener" {
  load_balancer_arn = "${aws_alb.main.id}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.api.arn}"

  default_action {
    target_group_arn = "${aws_alb_target_group.platform.id}"
    type             = "forward"
  }
}

resource "aws_alb_listener" "jenkins_listener" {
  load_balancer_arn = "${aws_alb.tools.id}"
  port              = "443"
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${data.aws_acm_certificate.tools.arn}"

  default_action {
    target_group_arn = "${aws_alb_target_group.jenkins.id}"
    type             = "forward"
  }
}

resource "aws_alb_target_group" "platform" {
  name     = "platform-alb-target-group"
  port     = 80
  protocol = "HTTP"
  vpc_id   = "${aws_vpc.main.id}"
  health_check {
    path = "/management/healthcheck"
  }
}

resource "aws_alb_target_group" "jenkins" {
  name     = "jenkins-alb-target-group"
  port     = 80
  protocol = "HTTP"
  vpc_id   = "${aws_vpc.tools.id}"
  health_check {
    path = "/login"
  }
}
