module "platform" {
  source = "./platform"

  repo_name = "platform"

  infra_bucket_arn = "${local.infra_bucket_arn}"

  sbt_releases_bucket_arn = "${aws_s3_bucket.releases.arn}"

  lambda_pushes_topic_name = "${module.ecr_pushes_topic.name}"
  ecr_pushes_topic_name    = "${module.lambda_pushes_topic.name}"
}

module "catalogue_repo" {
  source = "./platform"

  repo_name = "catalogue"

  infra_bucket_arn = "${local.infra_bucket_arn}"

  sbt_releases_bucket_arn = "${aws_s3_bucket.releases.arn}"

  lambda_pushes_topic_name = "${module.ecr_pushes_topic.name}"
  ecr_pushes_topic_name    = "${module.lambda_pushes_topic.name}"

  providers = {
    aws = "aws.catalogue"
  }
}

module "storage_repo" {
  source = "./platform"

  repo_name = "storage"

  infra_bucket_arn = "${local.infra_bucket_arn}"

  sbt_releases_bucket_arn = "${aws_s3_bucket.releases.arn}"

  lambda_pushes_topic_name = "${module.ecr_pushes_topic.name}"
  ecr_pushes_topic_name    = "${module.lambda_pushes_topic.name}"

  providers = {
    aws = "aws.storage"
  }
}

module "platform_cli" {
  source = "./python_library"

  repo_name     = "platform-cli"
  pypi_username = "${var.pypi_username}"
  pypi_password = "${var.pypi_password}"
}

module "scala_fixtures" {
  source = "./scala_library"

  name       = "fixtures"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
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

module "scala_typesafe" {
  source = "./scala_library"

  name       = "typesafe-app"
  repo_name  = "wellcome-typesafe-app"
  bucket_arn = "${aws_s3_bucket.releases.arn}"
}
