module "monitoring_alb" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_alb?ref=v1.0.0"
  name    = "monitoring"
  subnets = ["${module.vpc_monitoring.subnets}"]

  loadbalancer_security_groups = [
    "${module.monitoring_cluster_asg.loadbalancer_sg_https_id}",
    "${module.monitoring_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "monitoring.wellcomecollection.org"
  vpc_id             = "${module.vpc_monitoring.vpc_id}"

  alb_access_log_bucket = "${local.bucket_alb_logs_id}"
}
