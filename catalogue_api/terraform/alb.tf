module "load_balancer" {
  source = "load_balancer"

  name = "${local.namespace}"

  vpc_id         = "${local.vpc_id}"
  public_subnets = "${local.public_subnets}"

  default_target_group_arn = "${module.api_romulus_delta.target_group_arn}"
  certificate_domain       = "api.wellcomecollection.org"

  service_lb_security_group_ids = [
    "${module.api_romulus_delta.service_lb_security_group_id}",
    "${module.api_remus_delta.service_lb_security_group_id}",
  ]
}
