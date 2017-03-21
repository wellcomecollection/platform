module "services_alb" {
  source                       = "./ecs_alb"
  name                         = "services"
  subnets                      = ["${module.vpc_services.subnets}"]
  loadbalancer_security_groups = ["${module.services_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "${data.aws_acm_certificate.services.arn}"
  vpc_id                       = "${module.vpc_services.vpc_id}"
}

module "api_alb" {
  source                       = "./ecs_alb"
  name                         = "api"
  subnets                      = ["${module.vpc_api.subnets}"]
  loadbalancer_security_groups = ["${module.api_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "${data.aws_acm_certificate.api.arn}"
  vpc_id                       = "${module.vpc_api.vpc_id}"
}

module "tools_alb" {
  source                       = "./ecs_alb"
  name                         = "tools"
  subnets                      = ["${module.vpc_tools.subnets}"]
  loadbalancer_security_groups = ["${module.tools_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "${data.aws_acm_certificate.tools.arn}"
  vpc_id                       = "${module.vpc_tools.vpc_id}"
}
