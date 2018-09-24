data "terraform_remote_state" "catalogue_api" {
  backend = "s3"

  config {
    bucket = "wellcomecollection-platform-infra"
    key    = "terraform/catalogue_api.tfstate"
    region = "eu-west-1"
  }
}
