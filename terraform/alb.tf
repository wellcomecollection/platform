resource "aws_alb" "main" {
  name            = "platform-alb"
  subnets         = ["${module.vpc_main.subnets}"]
  security_groups = ["${aws_security_group.lb_sg.id}"]
}

resource "aws_alb" "tools" {
  name            = "platform-tools-alb"
  subnets         = ["${module.vpc_tools.subnets}"]
  security_groups = ["${aws_security_group.tools_lb_sg.id}"]
}
