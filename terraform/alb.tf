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
