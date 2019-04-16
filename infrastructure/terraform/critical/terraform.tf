terraform {
  required_version = ">= 0.9"

  backend "s3" {
    role_arn = "arn:aws:iam::760097843905:role/developer"

    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/catalogue_pipeline_data.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
