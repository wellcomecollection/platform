module "platform" {
  source    = "./platform"
  repo_name = "platform"

  sbt_releases_bucket_arn = aws_s3_bucket.releases.arn
  infra_bucket_arn        = local.infra_bucket_arn

  publish_topics = [
    aws_sns_topic.ecr_pushes.arn,
    aws_sns_topic.lambda_pushes.arn,
  ]

  platform_read_only_role = var.platform_read_only_role

  providers = {
    aws = aws.platform
  }
}

module "catalogue_repo" {
  source    = "./platform"
  repo_name = "catalogue"

  sbt_releases_bucket_arn = aws_s3_bucket.releases.arn
  infra_bucket_arn        = local.infra_bucket_arn

  publish_topics = [
    aws_sns_topic.ecr_pushes.arn,
    aws_sns_topic.lambda_pushes.arn,
  ]

  platform_read_only_role = var.platform_read_only_role

  providers = {
    aws = aws.catalogue
  }
}

module "storage_repo" {
  source    = "./platform"
  repo_name = "storage"

  sbt_releases_bucket_arn = aws_s3_bucket.releases.arn
  infra_bucket_arn        = local.infra_bucket_arn

  publish_topics = [
    aws_sns_topic.ecr_pushes.arn,
    aws_sns_topic.lambda_pushes.arn,
  ]

  platform_read_only_role = var.platform_read_only_role

  providers = {
    aws = aws.storage
  }
}

module "stacks_service_repo" {
  source    = "./platform"
  repo_name = "stacks-service"

  sbt_releases_bucket_arn = aws_s3_bucket.releases.arn
  infra_bucket_arn        = local.infra_bucket_arn

  publish_topics = [
    aws_sns_topic.ecr_pushes.arn,
    aws_sns_topic.lambda_pushes.arn,
  ]

  platform_read_only_role = var.platform_read_only_role

  providers = {
    aws = aws.catalogue
  }
}

module "archivematica_repo" {
  source    = "./platform"
  repo_name = "archivematica-infra"

  sbt_releases_bucket_arn = aws_s3_bucket.releases.arn
  infra_bucket_arn        = local.infra_bucket_arn

  publish_topics = [
    aws_sns_topic.ecr_pushes.arn,
    aws_sns_topic.lambda_pushes.arn,
  ]

  platform_read_only_role = var.platform_read_only_role

  providers = {
    aws = aws.workflow
  }
}

module "scala_fixtures" {
  source = "./scala_library"

  name       = "fixtures"
  bucket_arn = aws_s3_bucket.releases.arn
}

module "scala_json" {
  source = "./scala_library"

  name       = "json"
  bucket_arn = aws_s3_bucket.releases.arn
}

module "scala_monitoring" {
  source = "./scala_library"

  name       = "monitoring"
  bucket_arn = aws_s3_bucket.releases.arn
}

module "scala_storage" {
  source = "./scala_library"

  name       = "storage"
  bucket_arn = aws_s3_bucket.releases.arn
}

module "scala_messaging" {
  source = "./scala_library"

  name       = "messaging"
  bucket_arn = aws_s3_bucket.releases.arn
}

module "scala_typesafe" {
  source = "./scala_library"

  name       = "typesafe-app"
  repo_name  = "wellcome-typesafe-app"
  bucket_arn = aws_s3_bucket.releases.arn
}
