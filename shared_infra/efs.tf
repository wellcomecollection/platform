module "grafana_efs" {
  name                         = "grafana"
  source                       = "../terraform/efs"
  vpc_id                       = "${module.vpc_monitoring.vpc_id}"
  subnets                      = "${module.vpc_monitoring.subnets}"
  efs_access_security_group_id = "${module.monitoring_cluster_asg.instance_sg_id}"
}

module "loris_efs" {
  name                         = "loris_cache"
  source                       = "../terraform/efs"
  vpc_id                       = "${module.vpc_api.vpc_id}"
  subnets                      = "${module.vpc_api.subnets}"
  efs_access_security_group_id = "${module.api_cluster_asg.instance_sg_id}"
  performance_mode             = "maxIO"
}
