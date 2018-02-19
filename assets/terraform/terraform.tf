terraform {
  required_version = ">= 0.11"

  backend "s3" {
    bucket         = "platform-infra"
    key            = "terraform/assets.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
