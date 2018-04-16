terraform {
  required_version = ">= 0.10"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/monitoring.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "catalogue_pipeline" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/catalogue_pipeline.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "loris_cloudfront_monitoring" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/loris_cloudfront_monitoring.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/shared_infra.tfstate"
    region = "eu-west-1"
  }
}
