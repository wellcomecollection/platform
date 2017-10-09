module "grafana_efs" {
  name                         = "grafana"
  source                       = "../terraform/efs"
  vpc_id                       = "${module.vpc_monitoring.vpc_id}"
  subnets                      = "${module.vpc_monitoring.subnets}"
  efs_access_security_group_ids = ["${module.monitoring_cluster_asg.instance_sg_id}"]
}
