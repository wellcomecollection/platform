locals {
  vpc_id          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_id}"
  private_subnets = "${data.terraform_remote_state.shared_infra.catalogue_private_subnets}"
  public_subnets  = "${data.terraform_remote_state.shared_infra.catalogue_public_subnets}"
}
