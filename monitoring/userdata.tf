module "monitoring_userdata" {
  source            = "git::https://github.com/wellcometrust/terraform.git//userdata?ref=v1.0.0"
  cluster_name      = "${aws_ecs_cluster.monitoring.name}"
  efs_filesystem_id = "${module.grafana_efs.efs_id}"
}
