terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "terraform/sierra_adapter.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
