locals {
  aws_region   = "eu-west-1"
  infra_bucket = "${aws_s3_bucket.platform_infra.id}"
}
