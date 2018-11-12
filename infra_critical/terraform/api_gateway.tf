module "domain_prod" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=290a16393da3018435edcb26ba144582b6fb1b2b"

  domain_name      = "api.wellcomecollection.org"
  cert_domain_name = "api.wellcomecollection.org"
}

module "domain_stage" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=290a16393da3018435edcb26ba144582b6fb1b2b"

  domain_name      = "api-stage.wellcomecollection.org"
  cert_domain_name = "api.wellcomecollection.org"
}
