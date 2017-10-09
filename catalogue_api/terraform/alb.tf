module "api_alb" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_alb?ref=v1.0.0"
  name    = "api"
  subnets = ["${module.vpc_api.subnets}"]

  loadbalancer_security_groups = [
    "${module.api_cluster_asg.loadbalancer_sg_https_id}",
    "${module.api_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "api.wellcomecollection.org"
  vpc_id             = "${module.vpc_api.vpc_id}"

  alb_access_log_bucket = "${aws_s3_bucket.alb-logs.id}"
}
