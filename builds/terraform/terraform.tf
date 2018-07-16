terraform {
  required_version = ">= 0.11"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/builds.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
