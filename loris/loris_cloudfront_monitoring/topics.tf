module "cloudfront_errors_topic" {
  source = "git::https://github.com/wellcometrust/terraform.git//sns?ref=v1.0.0"
  name   = "cloudfront_errors"
}
