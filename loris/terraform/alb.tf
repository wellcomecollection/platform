module "loris_alb" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_alb?ref=v1.0.0"
  name    = "loris"
  subnets = ["${local.vpc_api_subnets}"]

  loadbalancer_security_groups = [
    "${module.loris_cluster_asg.loadbalancer_sg_https_id}",
    "${module.loris_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${local.vpc_api_id}"

  alb_access_log_bucket = "${local.bucket_alb_logs_id}"
}

module "loris_alb_ebs" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_alb?ref=v1.0.0"
  name    = "loris-ebs"
  subnets = ["${local.vpc_api_subnets}"]

  loadbalancer_security_groups = [
    "${module.loris_cluster_asg_ebs.loadbalancer_sg_https_id}",
    "${module.loris_cluster_asg_ebs.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${local.vpc_api_id}"

  alb_access_log_bucket = "${local.bucket_alb_logs_id}"
}
