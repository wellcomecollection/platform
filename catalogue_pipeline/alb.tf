module "services_alb" {
  source  = "git::https://github.com/wellcometrust/terraform.git//ecs_alb?ref=ecs-sqs-autoscaling-policy"
  name    = "services"
  subnets = ["${module.vpc_services.subnets}"]

  loadbalancer_security_groups = [
    "${module.services_cluster_asg.loadbalancer_sg_https_id}",
    "${module.services_cluster_asg.loadbalancer_sg_http_id}",
    "${module.services_cluster_asg_on_demand.loadbalancer_sg_https_id}",
    "${module.services_cluster_asg_on_demand.loadbalancer_sg_http_id}",
  ]

  certificate_domain = "services.wellcomecollection.org"
  vpc_id             = "${module.vpc_services.vpc_id}"

  alb_access_log_bucket = "${local.bucket_alb_logs_id}"
}
