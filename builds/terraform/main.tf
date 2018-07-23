module "platform" {
  source = "./platform"

  infra_bucket_arn = "${local.infra_bucket_arn}"

  sbt_releases_bucket_arn = "${aws_s3_bucket.releases.arn}"

  lambda_pushes_topic_name = "${local.ecr_pushes_topic_name}"
  ecr_pushes_topic_name    = "${local.lambda_pushes_topic_name}"
}

module "scala_storage" {
  source = "./scala_library"

  name       = "storage"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
}
