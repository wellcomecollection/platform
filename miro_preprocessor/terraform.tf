terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "terraform/miro_preprocessor.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "catalogue_pipeline" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "terraform/catalogue_pipeline.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "catalogue_api" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "terraform/catalogue_api.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "loris" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "terraform/loris.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "shared_infra" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "terraform/shared_infra.tfstate"
    region = "eu-west-1"
  }
}
