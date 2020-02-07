terraform {
  required_version = ">= 0.9"

  backend "s3" {
    bucket         = "wellcomecollection-platform-infra"
    key            = "terraform/platform-private.tfstate"
    dynamodb_table = "terraform-locktable"

    role_arn = "arn:aws:iam::760097843905:role/platform-developer"
    region   = "eu-west-1"
  }
}

data "terraform_remote_state" "storage" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::975596993436:role/storage-read_only"


    bucket = "wellcomecollection-storage-infra"
    key    = "terraform/storage-private.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "experience" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::130871440101:role/experience-read_only"

    bucket = "wellcomecollection-infra"
    key    = "build-state/terraform-config.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "data" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::964279923020:role/data-read_only"

    bucket = "wellcomecollection-datascience-infra"
    key    = "terraform/datascience-private.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "reporting" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::269807742353:role/reporting-read_only"

    bucket = "wellcomecollection-reporting-infra"
    key    = "terraform/reporting-private.tfstate"
    region = "eu-west-1"
  }
}

data "terraform_remote_state" "digitisation" {
  backend = "s3"

  config = {
    role_arn = "arn:aws:iam::404315009621:role/digitisation-read_only"

    bucket = "wellcomedigitisation-infra"
    key    = "terraform/digitisation-private.tfstate"
    region = "eu-west-1"
  }
}

data "aws_caller_identity" "current" {}

data "template_file" "pgp_key" {
  template = file("${path.module}/wellcomedigitalplatform.pub")
}

locals {
  goobi_role_arn        = "arn:aws:iam::299497370133:role/goobi_task_role"
  itm_role_arn          = "arn:aws:iam::299497370133:role/itm_task_role"
  shell_server_role_arn = "arn:aws:iam::299497370133:role/shell_server_task_role"

  intranda_export_bucket = "wellcomecollection-workflow-export-bagit"
  dds_principal_arn      = "arn:aws:iam::653428163053:user/dlcs-dds"

  account_id    = data.aws_caller_identity.current.account_id
  aws_principal = "arn:aws:iam::${local.account_id}:root"
}
