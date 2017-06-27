module "services_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.services.name}"
}

module "monitoring_userdata" {
  source            = "./userdata"
  cluster_name      = "${aws_ecs_cluster.monitoring.name}"
  efs_filesystem_id = "${module.grafana_efs.efs_id}"
  template_name     = "ecs-agent-with-efs-broken"
}

module "api_userdata" {
  source       = "./userdata"
  cluster_name = "${aws_ecs_cluster.api.name}"
}
