module "services_userdata" {
  source       = "../terraform/userdata"
  cluster_name = "${aws_ecs_cluster.services.name}"
}

module "monitoring_userdata" {
  source            = "../terraform/userdata"
  cluster_name      = "${aws_ecs_cluster.monitoring.name}"
  efs_filesystem_id = "${module.grafana_efs.efs_id}"
}

module "api_userdata" {
  source            = "../terraform/userdata"
  cluster_name      = "${aws_ecs_cluster.api.name}"
  efs_filesystem_id = "${module.loris_efs.efs_id}"
}
