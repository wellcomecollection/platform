module "platform_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.main.name}"
}

module "tools_userdata" {
  source        = "./userdata"
  template_name = "ecs-agent-tools"
  cluster_name  = "${aws_ecs_cluster.main.name}"
}
