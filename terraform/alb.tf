module "services_alb" {
  source                       = "./ecs_alb"
  name                         = "services"
  subnets                      = ["${module.vpc_services.subnets}"]
  loadbalancer_security_groups = ["${module.services_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "${data.aws_acm_certificate.services.arn}"
  vpc_id                       = "${module.vpc_services.vpc_id}"
}

module "monitoring_alb" {
  source                       = "./ecs_alb"
  name                         = "monitoring"
  subnets                      = ["${module.vpc_monitoring.subnets}"]
  loadbalancer_security_groups = ["${module.monitoring_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "${data.aws_acm_certificate.monitoring.arn}"
  vpc_id                       = "${module.vpc_monitoring.vpc_id}"
}

module "api_alb" {
  source                       = "./ecs_alb"
  name                         = "api"
  subnets                      = ["${module.vpc_api.subnets}"]
  loadbalancer_security_groups = ["${module.api_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "${data.aws_acm_certificate.api.arn}"
  vpc_id                       = "${module.vpc_api.vpc_id}"
}
