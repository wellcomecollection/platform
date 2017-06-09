resource "aws_alb" "ecs_service" {
  name            = "${var.name}"
  subnets         = ["${var.subnets}"]
  security_groups = ["${var.loadbalancer_security_groups}"]

  lifecycle {
    prevent_destroy = true
  }
}

resource "aws_alb_listener" "ecs_service" {
  load_balancer_arn = "${aws_alb.ecs_service.id}"
  port              = "${var.listener_port}"
  protocol          = "${var.listener_protocol}"
  ssl_policy        = "ELBSecurityPolicy-2015-05"
  certificate_arn   = "${var.certificate_arn}"

  default_action {
    target_group_arn = "${aws_alb_target_group.ecs_service_default.arn}"
    type             = "forward"
  }
}

resource "aws_alb_target_group" "ecs_service_default" {
  name     = "${var.name}-default-target-group"
  port     = "${var.target_group_port}"
  protocol = "HTTP"
  vpc_id   = "${var.vpc_id}"

  health_check {
    path = "${var.health_check_path}"
  }
}
