resource "aws_sns_topic" "cloudfront_errors" {
  name = "cloudfront_errors"

  provider = "aws.us_east_1"
}
