resource "aws_alb" "main" {
  name            = "platform-alb"
  subnets         = ["${module.vpc_main.subnets}"]
  security_groups = ["${module.platform_cluster_asg.loadbalancer_sg_id}"]
}

resource "aws_alb" "tools" {
  name            = "platform-tools-alb"
  subnets         = ["${module.vpc_tools.subnets}"]
  security_groups = ["${module.tools_cluster_asg.loadbalancer_sg_id}"]
}
