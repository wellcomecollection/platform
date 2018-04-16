module "loris_alb" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_alb?ref=v1.0.0"
  name    = "loris"
  subnets = ["${module.vpc_loris.subnets}"]

  loadbalancer_security_groups = [
    "${module.loris_cluster_asg.loadbalancer_sg_https_id}",
    "${module.loris_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${module.vpc_loris.vpc_id}"

  alb_access_log_bucket = "${local.bucket_alb_logs_id}"
}
