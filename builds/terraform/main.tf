module "platform" {
  source = "./platform"

  infra_bucket_arn = "${local.infra_bucket_arn}"

  sbt_releases_bucket_arn = "${aws_s3_bucket.releases.arn}"

  lambda_pushes_topic_name = "${module.ecr_pushes_topic.name}"
  ecr_pushes_topic_name    = "${module.lambda_pushes_topic.name}"
}

module "platform_cli" {
  source = "./python_library"

  repo_name     = "platform-cli"
  pypi_username = "${var.pypi_username}"
  pypi_password = "${var.pypi_password}"
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

module "scala_messaging" {
  source = "./scala_library"

  name       = "messaging"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
}