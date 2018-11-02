terraform {
  required_version = ">= 0.11"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/builds.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
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
