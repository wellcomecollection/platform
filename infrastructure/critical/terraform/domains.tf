module "domain_prod" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=v18.2.3"

  domain_name      = "api.wellcomecollection.org"
  cert_domain_name = "api.wellcomecollection.org"
}

module "domain_stage" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=v18.2.3"

  domain_name      = "api-stage.wellcomecollection.org"
  cert_domain_name = "api.wellcomecollection.org"
}
