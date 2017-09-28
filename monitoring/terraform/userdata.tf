module "monitoring_userdata" {
  source            = "../terraform/userdata"
  cluster_name      = "${aws_ecs_cluster.monitoring.name}"
  efs_filesystem_id = "${module.grafana_efs.efs_id}"
}
