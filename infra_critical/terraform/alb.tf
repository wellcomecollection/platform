module "load_balancer" {
  source = "load_balancer"

  name = "api-wellcomecollection-org"

  vpc_id         = "${local.vpc_id}"
  public_subnets = "${local.public_subnets}"

  certificate_domain = "api.wellcomecollection.org"
}
