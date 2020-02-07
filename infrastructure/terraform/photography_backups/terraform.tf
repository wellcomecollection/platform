terraform {
  required_version = ">= 0.11"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/platform-developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/platform-private/backups.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
