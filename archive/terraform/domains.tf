data "aws_acm_certificate" "storage_api" {
  domain   = "storage.api.wellcomecollection.org"
  statuses = ["ISSUED"]
}