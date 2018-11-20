locals {
  vpc_id          = "vpc-f7377891"
  public_subnets  = [
    "subnet-809503c8",
    "subnet-8d9a5ad7",
    "subnet-59f97f3f"
  ]

  private_subnets = [
    "subnet-999701d1",
    "subnet-069c5c5c",
    "subnet-cefc7aa8"
  ]

  vpc_id_new          = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_id}"
  public_subnets_new  = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_public_subnets}"
  private_subnets_new = "${data.terraform_remote_state.shared_infra.catalogue_vpc_delta_private_subnets}"
}
