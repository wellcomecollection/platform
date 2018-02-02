terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "platform-pipeline.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
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
