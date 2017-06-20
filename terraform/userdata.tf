module "services_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.services.name}"
}

module "monitoring_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.monitoring.name}"
  efs_filesystem_id = "${aws_efs_file_system.grafana_efs.id}"
  template_name = "ecs-agent-with-efs"
}

module "api_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.api.name}"
}
