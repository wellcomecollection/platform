terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/catalogue_pipeline_data.tfstate"
    dynamodb_table = "terraform-locktable"
    region         = "eu-west-1"
  }
}
