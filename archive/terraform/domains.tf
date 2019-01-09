data "aws_acm_certificate" "storage_api" {
  domain   = "storage.api.wellcomecollection.org"
  statuses = ["ISSUED"]
}

module "storage_api_domain_prod" {
  source = "git::https://github.com/wellcometrust/terraform.git//api_gateway/modules/domain?ref=290a16393da3018435edcb26ba144582b6fb1b2b"

  domain_name      = "storage.api.wellcomecollection.org"
  cert_domain_name = "storage.api.wellcomecollection.org"
}
