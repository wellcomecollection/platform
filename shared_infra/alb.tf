module "services_alb" {
  source  = "../terraform/ecs_alb"
  name    = "services"
  subnets = ["${module.vpc_services.subnets}"]

  loadbalancer_security_groups = [
    "${module.services_cluster_asg.loadbalancer_sg_https_id}",
    "${module.services_cluster_asg.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "services.wellcomecollection.org"
  vpc_id             = "${module.vpc_services.vpc_id}"

  alb_access_log_bucket = "${aws_s3_bucket.alb-logs.id}"
}

module "api_alb" {
  source  = "../terraform/ecs_alb"
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
