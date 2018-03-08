terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/data_api.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
