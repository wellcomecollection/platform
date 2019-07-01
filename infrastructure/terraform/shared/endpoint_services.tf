locals {
  storage_account_id = "975596993436"
  storage_principal  = "arn:aws:iam::${local.storage_account_id}:root"

  experience_account_id = "130871440101"
  experience_principal  = "arn:aws:iam::${local.experience_account_id}:root"
}

data "aws_lb" "pl-winslow" {
  name = "pl-winslow"
}

data "aws_lb" "wt-winnipeg" {
  name = "wt-winnipeg"
}

data "aws_lb" "libsys" {
  name = "libsys"
}

resource "aws_vpc_endpoint_service" "pl-winslow" {
  acceptance_required        = false
  network_load_balancer_arns = ["${data.aws_lb.pl-winslow.arn}"]

  allowed_principals = [
    "${local.storage_principal}",
  ]
}

resource "aws_vpc_endpoint_service" "wt-winnipeg" {
  acceptance_required        = false
  network_load_balancer_arns = ["${data.aws_lb.wt-winnipeg.arn}"]

  allowed_principals = [
    "${local.storage_principal}",
  ]
}

resource "aws_vpc_endpoint_service" "libsys" {
  acceptance_required        = false
  network_load_balancer_arns = ["${data.aws_lb.libsys.arn}"]

  allowed_principals = [
    "${local.experience_principal}",
  ]
}