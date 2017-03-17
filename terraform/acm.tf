data "aws_acm_certificate" "tools" {
  domain   = "*.wellcome-tools.org"
  statuses = ["ISSUED"]
}

data "aws_acm_certificate" "api" {
  domain   = "api.wellcomecollection.org"
  statuses = ["ISSUED"]
}
