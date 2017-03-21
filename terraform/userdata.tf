module "services_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.services.name}"
}

module "api_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.api.name}"
}

module "tools_userdata" {
  source        = "./userdata"
  template_name = "ecs-agent-tools"
  cluster_name  = "${aws_ecs_cluster.tools.name}"
  efs_fs_name   = "${aws_efs_file_system.jenkins.id}"
}
