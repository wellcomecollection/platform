module "platform" {
  source = "./platform"

  infra_bucket_arn = "${local.infra_bucket_arn}"

  sbt_releases_bucket_arn = "${aws_s3_bucket.releases.arn}"

  lambda_pushes_topic_name = "${local.ecr_pushes_topic_name}"
  ecr_pushes_topic_name    = "${local.lambda_pushes_topic_name}"
}

module "scala_json" {
  source = "./scala_library"

  name       = "json"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
}

module "scala_monitoring" {
  source = "./scala_library"

  name       = "monitoring"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
}

module "scala_storage" {
  source = "./scala_library"

  name       = "storage"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
}
