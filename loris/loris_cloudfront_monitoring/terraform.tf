terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/loris-cloudfront-monitoring.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}

data "terraform_remote_state" "loris" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/loris.tfstate"
    region = "eu-west-1"
  }
}
