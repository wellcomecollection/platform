terraform {
  required_version = ">= 0.10"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "terraform/monitoring.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "catalogue_pipeline" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform-pipeline.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform-lambda.tfstate"
    region = "eu-west-1"
  }
}
