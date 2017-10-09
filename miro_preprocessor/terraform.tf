terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "platform-miro_preprocessor.tfstate"
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

data "terraform_remote_state" "loris" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform-loris.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "lambda" {
  backend = "s3"

  config {
    bucket = "platform-infra"
    key    = "platform-lambda.tfstate"
    region = "eu-west-1"
  }
}
