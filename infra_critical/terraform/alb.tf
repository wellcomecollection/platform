module "load_balancer" {
  source = "load_balancer"

  name = "api-wellcomecollection-org"

  vpc_id         = "${local.vpc_id}"
  public_subnets = "${local.public_subnets}"

  certificate_domain = "api.wellcomecollection.org"

  top_level_host = "developers.wellcomecollection.org"
  top_level_path = "/"
}
