terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "platform-lambda.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "platform" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform.tfstate"
    region = "eu-west-1"
  }
}
