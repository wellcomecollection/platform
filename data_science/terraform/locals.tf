locals {
  vpc_id          = "${data.terraform_remote_state.shared_infra.datascience_vpc_delta_id}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.datascience_vpc_delta_public_subnets}"
  private_subnets = "${data.terraform_remote_state.shared_infra.datascience_vpc_delta_private_subnets}"

  namespace = "datascience"
}

data "aws_vpc" "datascience" {
  id = "${local.vpc_id}"
}
