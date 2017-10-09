module "grafana_efs" {
  name                         = "grafana"
  source                       = "git::https://github.com/wellcometrust/terraform.git//efs?ref=v1.0.0"
  vpc_id                       = "${module.vpc_monitoring.vpc_id}"
  subnets                      = "${module.vpc_monitoring.subnets}"
  efs_access_security_group_id = "${module.monitoring_cluster_asg.instance_sg_id}"
}
