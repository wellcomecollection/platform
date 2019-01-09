data "aws_acm_certificate" "catalogue_api" {
  domain   = "catalogue.api.wellcomecollection.org"
  statuses = ["ISSUED"]
}

module "catalogue_api_domain_prod" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=v18.2.3"

  domain_name      = "catalogue.api.wellcomecollection.org"
  cert_domain_name = "catalogue.api.wellcomecollection.org"
}

module "catalogue_api_domain_stage" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=v18.2.3"

  domain_name      = "catalogue.api-stage.wellcomecollection.org"
  cert_domain_name = "catalogue.api.wellcomecollection.org"
}
