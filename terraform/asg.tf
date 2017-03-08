module "platform_cluster_asg" {
  source             = "./ecs_asg"
  asg_name           = "platform-ecs-cluster"
  asg_min            = "1"
  asg_desired        = "1"
  asg_max            = "2"
  launch_config_name = "${aws_launch_configuration.platform.name}"
  subnet_list        = ["${module.vpc_main.subnets}"]
}

module "tools_cluster_asg" {
  source             = "./ecs_asg"
  asg_name           = "tools-ecs-cluster"
  asg_min            = "1"
  asg_desired        = "1"
  asg_max            = "2"
  launch_config_name = "${aws_launch_configuration.tools.name}"
  subnet_list        = ["${module.vpc_tools.subnets}"]
}
