data "aws_acm_certificate" "catalogue_api" {
  domain   = "catalogue.api.wellcomecollection.org"
  statuses = ["ISSUED"]
}

module "catalogue_api_domain_prod" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=290a16393da3018435edcb26ba144582b6fb1b2b"

  domain_name      = "catalogue.api.wellcomecollection.org"
  cert_domain_name = "catalogue.api.wellcomecollection.org"
}

module "catalogue_api_domain_stage" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=290a16393da3018435edcb26ba144582b6fb1b2b"

  domain_name      = "catalogue.api-stage.wellcomecollection.org"
  cert_domain_name = "catalogue.api.wellcomecollection.org"
}